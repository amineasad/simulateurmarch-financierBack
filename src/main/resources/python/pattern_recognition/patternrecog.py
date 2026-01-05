import yfinance as yf
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.svm import SVC
from sklearn.model_selection import train_test_split, TimeSeriesSplit
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.cluster import KMeans
from xgboost import XGBClassifier
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout
import warnings
warnings.filterwarnings('ignore')

class MLTrendPatternDetector:
    def __init__(self, symbol, period="2y"):
        self.symbol = symbol
        self.period = period
        self.data = self._download_data()
        self.scaler = StandardScaler()
        self.models = {}
        
    def _download_data(self):
        """Télécharge et prépare les données"""
        print(f"📥 Téléchargement des données pour {self.symbol}...")
        ticker = yf.Ticker(self.symbol)
        data = ticker.history(period=self.period)
        
        if data.empty:
            raise ValueError(f"Aucune donnée trouvée pour {self.symbol}")
            
        print(f"✅ Données téléchargées: {len(data)} points")
        return data
    
    def create_technical_features(self):
        """Crée des features techniques pour le ML"""
        df = self.data.copy()
        
        # Features de prix
        df['Returns'] = df['Close'].pct_change()
        df['Price_Change'] = df['Close'].diff()
        df['High_Low_Ratio'] = df['High'] / df['Low']
        df['Open_Close_Ratio'] = df['Open'] / df['Close']
        
        # Moyennes mobiles et ratios
        for window in [5, 10, 20, 50]:
            df[f'MA_{window}'] = df['Close'].rolling(window).mean()
            df[f'Price_MA_Ratio_{window}'] = df['Close'] / df[f'MA_{window}']
            df[f'MA_Ratio_{window}'] = df[f'MA_{window}'] / df[f'MA_{window}'].shift(5)
        
        # Volatilité
        df['Volatility_5'] = df['Returns'].rolling(5).std()
        df['Volatility_20'] = df['Returns'].rolling(20).std()
        df['Volume_MA'] = df['Volume'].rolling(20).mean()
        df['Volume_Ratio'] = df['Volume'] / df['Volume_MA']
        
        # RSI
        df['RSI'] = self._calculate_rsi(df['Close'])
        
        # MACD
        exp12 = df['Close'].ewm(span=12).mean()
        exp26 = df['Close'].ewm(span=26).mean()
        df['MACD'] = exp12 - exp26
        df['MACD_Signal'] = df['MACD'].ewm(span=9).mean()
        
        # Bollinger Bands
        df['BB_Middle'] = df['Close'].rolling(20).mean()
        bb_std = df['Close'].rolling(20).std()
        df['BB_Upper'] = df['BB_Middle'] + (bb_std * 2)
        df['BB_Lower'] = df['BB_Middle'] - (bb_std * 2)
        df['BB_Position'] = (df['Close'] - df['BB_Lower']) / (df['BB_Upper'] - df['BB_Lower'])
        
        # Momentum
        df['Momentum_5'] = df['Close'] / df['Close'].shift(5) - 1
        df['Momentum_10'] = df['Close'] / df['Close'].shift(10) - 1
        
        # Features de tendance
        df['Trend_Strength'] = self._calculate_trend_strength(df)
        
        # Target variable: Direction future (1 = haussier, 0 = baissier)
        df['Future_Return_5'] = df['Close'].shift(-5) / df['Close'] - 1
        df['Target'] = (df['Future_Return_5'] > 0.02).astype(int)  # +2% dans 5 jours
        
        # Supprimer les NaN
        df = df.dropna()
        
        self.feature_data = df
        return df
    
    def _calculate_rsi(self, prices, window=14):
        """Calcule le RSI"""
        delta = prices.diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=window).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=window).mean()
        rs = gain / loss
        return 100 - (100 / (1 + rs))
    
    def _calculate_trend_strength(self, df):
        """Calcule la force de la tendance"""
        # Différence entre MAs court terme et long terme
        ma_diff = (df['MA_5'] - df['MA_20']) / df['MA_20']
        # Pente de la regression linéaire
        prices = df['Close'].values
        x = np.arange(len(prices))
        slope = np.polyfit(x, prices, 1)[0]
        return slope / prices.mean()
    
    def prepare_ml_data(self):
        """Prépare les données pour le machine learning"""
        df = self.feature_data.copy()
        
        # Sélection des features
        feature_columns = [col for col in df.columns if col not in 
                          ['Target', 'Future_Return_5', 'Open', 'High', 'Low', 'Close', 'Volume']]
        
        X = df[feature_columns]
        y = df['Target']
        
        # Split temporel (pas de shuffle pour les séries temporelles)
        split_index = int(len(X) * 0.8)
        X_train, X_test = X[:split_index], X[split_index:]
        y_train, y_test = y[:split_index], y[split_index:]
        
        # Normalisation
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        return X_train_scaled, X_test_scaled, y_train, y_test, feature_columns
    
    def train_models(self):
        """Entraîne plusieurs modèles de ML"""
        X_train, X_test, y_train, y_test, feature_columns = self.prepare_ml_data()
        
        models = {
            'Random Forest': RandomForestClassifier(n_estimators=100, random_state=42),
            'XGBoost': XGBClassifier(random_state=42),
            'SVM': SVC(probability=True, random_state=42),
            'Gradient Boosting': GradientBoostingClassifier(random_state=42)
        }
        
        results = {}
        
        for name, model in models.items():
            print(f"🤖 Entraînement du modèle: {name}")
            
            # Entraînement
            model.fit(X_train, y_train)
            
            # Prédictions
            y_pred = model.predict(X_test)
            y_pred_proba = model.predict_proba(X_test)[:, 1]
            
            # Évaluation
            accuracy = accuracy_score(y_test, y_pred)
            
            results[name] = {
                'model': model,
                'accuracy': accuracy,
                'predictions': y_pred,
                'probabilities': y_pred_proba,
                'feature_importance': getattr(model, 'feature_importances_', None)
            }
            
            print(f"✅ {name} - Accuracy: {accuracy:.3f}")
        
        self.models = results
        self.best_model_name = max(results, key=lambda x: results[x]['accuracy'])
        print(f"\n🏆 Meilleur modèle: {self.best_model_name}")
        
        return results
    
    def create_lstm_model(self, lookback=30):
        """Crée et entraîne un modèle LSTM pour la prédiction de séries temporelles"""
        df = self.feature_data.copy()
        
        # Préparation des données pour LSTM
        feature_columns = [col for col in df.columns if col not in 
                          ['Target', 'Future_Return_5', 'Open', 'High', 'Low', 'Close', 'Volume']]
        
        X = df[feature_columns].values
        y = df['Target'].values
        
        # Création des séquences
        X_seq, y_seq = [], []
        for i in range(lookback, len(X)):
            X_seq.append(X[i-lookback:i])
            y_seq.append(y[i])
        
        X_seq, y_seq = np.array(X_seq), np.array(y_seq)
        
        # Split
        split_idx = int(len(X_seq) * 0.8)
        X_train, X_test = X_seq[:split_idx], X_seq[split_idx:]
        y_train, y_test = y_seq[:split_idx], y_seq[split_idx:]
        
        # Normalisation
        X_train_reshaped = X_train.reshape(-1, X_train.shape[2])
        X_test_reshaped = X_test.reshape(-1, X_test.shape[2])
        
        X_train_scaled = self.scaler.fit_transform(X_train_reshaped)
        X_test_scaled = self.scaler.transform(X_test_reshaped)
        
        X_train_scaled = X_train_scaled.reshape(X_train.shape)
        X_test_scaled = X_test_scaled.reshape(X_test.shape)
        
        # Modèle LSTM
        model = Sequential([
            LSTM(50, return_sequences=True, input_shape=(lookback, X_train.shape[2])),
            Dropout(0.2),
            LSTM(50, return_sequences=False),
            Dropout(0.2),
            Dense(25, activation='relu'),
            Dense(1, activation='sigmoid')
        ])
        
        model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
        
        # Entraînement
        history = model.fit(
            X_train_scaled, y_train,
            batch_size=32,
            epochs=50,
            validation_data=(X_test_scaled, y_test),
            verbose=0
        )
        
        # Évaluation
        lstm_pred = (model.predict(X_test_scaled) > 0.5).astype(int).flatten()
        lstm_accuracy = accuracy_score(y_test, lstm_pred)
        
        self.models['LSTM'] = {
            'model': model,
            'accuracy': lstm_accuracy,
            'predictions': lstm_pred,
            'history': history
        }
        
        print(f"✅ LSTM - Accuracy: {lstm_accuracy:.3f}")
        return history
    
    def cluster_market_regimes(self, n_clusters=3):
        """Utilise K-means pour identifier les régimes de marché"""
        df = self.feature_data.copy()

        # Features pour le clustering (coerce to numeric to avoid object dtype issues)
        cluster_features = ['Returns', 'Volatility_20', 'Trend_Strength', 'RSI', 'Volume_Ratio']
        X_cluster = df[cluster_features].apply(pd.to_numeric, errors='coerce').dropna()

        # Clustering
        kmeans = KMeans(n_clusters=n_clusters, random_state=42)
        clusters = kmeans.fit_predict(X_cluster)

        # Analyse des clusters
        df_cluster = X_cluster.copy()
        df_cluster['Cluster'] = clusters
        df_cluster['Regime'] = df_cluster['Cluster'].map(self._interpret_clusters)

        # Aggregate only numeric columns to avoid errors from object dtypes (e.g., Regime)
        cluster_analysis = df_cluster.groupby('Cluster', sort=True).mean(numeric_only=True)

        self.cluster_results = {
            'clusters': clusters,
            'regimes': df_cluster['Regime'],
            'analysis': cluster_analysis
        }

        return cluster_analysis
    
    def _interpret_clusters(self, cluster):
        """Interprète les clusters comme régimes de marché"""
        regimes = {
            0: '📈 Haussière',
            1: '📉 Baissière', 
            2: '⚡ Volatile',
            3: '😐 Neutre'
        }
        return regimes.get(cluster, 'Inconnu')
    
    def generate_trading_signals(self):
        """Génère des signaux de trading basés sur les prédictions ML"""
        if not self.models:
            print("❌ Veuillez d'abord entraîner les modèles")
            return
        
        best_model = self.models[self.best_model_name]
        df = self.feature_data.copy()
        
        # Dernières prédictions
        X_train, X_test, y_train, y_test, feature_columns = self.prepare_ml_data()
        
        # Prédiction sur les données récentes
        recent_data = X_test[-30:]  # 30 derniers jours
        recent_predictions = best_model['model'].predict(recent_data)
        recent_probabilities = best_model['model'].predict_proba(recent_data)[:, 1]
        
        # Signaux
        current_signal = recent_predictions[-1]
        current_confidence = recent_probabilities[-1]
        
        # Analyse de consensus
        consensus = np.mean([self.models[name]['probabilities'][-1] for name in self.models 
                           if 'probabilities' in self.models[name]])
        
        signals = []
        
        if current_signal == 1 and current_confidence > 0.6:
            signals.append(('ACHAT', f'Confiance: {current_confidence:.1%}'))
        elif current_signal == 0 and current_confidence > 0.6:
            signals.append(('VENTE', f'Confiance: {current_confidence:.1%}'))
        else:
            signals.append(('NEUTRE', 'Attendre un signal plus fort'))
        
        return {
            'signal': signals[0][0],
            'confidence': current_confidence,
            'consensus': consensus,
            'details': signals
        }
    
    def comprehensive_analysis(self):
        """Effectue une analyse complète avec ML"""
        print(f"\n{'='*60}")
        print(f"🤖 ANALYSE ML AVANCÉE - {self.symbol}")
        print(f"{'='*60}")
        
        # Préparation des données
        self.create_technical_features()
        
        # Entraînement des modèles
        print("\n🧠 ENTRAÎNEMENT DES MODÈLES ML...")
        ml_results = self.train_models()
        
        # LSTM
        print("\n🔮 ENTRAÎNEMENT LSTM...")
        self.create_lstm_model()
        
        # Clustering des régimes
        print("\n🎯 ANALYSE DES RÉGIMES DE MARCHÉ...")
        regimes = self.cluster_market_regimes()
        print(regimes)
        
        # Signaux de trading
        print("\n🚨 GÉNÉRATION DES SIGNALS...")
        signals = self.generate_trading_signals()
        
        # Rapport final
        print(f"\n📊 RAPPORT FINAL ML:")
        print(f"   Meilleur modèle: {self.best_model_name}")
        print(f"   Accuracy: {ml_results[self.best_model_name]['accuracy']:.3f}")
        print(f"   Signal actuel: {signals['signal']}")
        print(f"   Confiance: {signals['confidence']:.1%}")
        print(f"   Consensus modèles: {signals['consensus']:.1%}")
        
        # Feature importance
        if self.best_model_name in ['Random Forest', 'XGBoost']:
            print(f"\n🎯 FEATURES IMPORTANTES:")
            feature_importance = self.models[self.best_model_name]['feature_importance']
            feature_names = [col for col in self.feature_data.columns if col not in 
                           ['Target', 'Future_Return_5', 'Open', 'High', 'Low', 'Close', 'Volume']]
            
            top_features = sorted(zip(feature_names, feature_importance), 
                                key=lambda x: x[1], reverse=True)[:5]
            
            for feature, importance in top_features:
                print(f"   {feature}: {importance:.3f}")
    
    def plot_ml_analysis(self):
        """Visualise les résultats du ML"""
        if not self.models:
            print("❌ Veuillez d'abord entraîner les modèles")
            return
        
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        
        # 1. Performance des modèles
        model_names = list(self.models.keys())
        accuracies = [self.models[name]['accuracy'] for name in model_names]
        
        axes[0, 0].bar(model_names, accuracies, color=['blue', 'green', 'orange', 'red', 'purple'])
        axes[0, 0].set_title('Performance des Modèles ML')
        axes[0, 0].set_ylabel('Accuracy')
        
        # 2. Prédictions vs Réalité
        best_model = self.models[self.best_model_name]
        X_train, X_test, y_train, y_test, _ = self.prepare_ml_data()
        
        axes[0, 1].plot(y_test.values[:50], label='Réel', marker='o')
        axes[0, 1].plot(best_model['predictions'][:50], label='Prédit', marker='x')
        axes[0, 1].set_title('Prédictions vs Réalité')
        axes[0, 1].legend()
        
        # 3. Feature Importance
        if self.best_model_name in ['Random Forest', 'XGBoost']:
            feature_importance = self.models[self.best_model_name]['feature_importance']
            feature_names = [col for col in self.feature_data.columns if col not in 
                           ['Target', 'Future_Return_5', 'Open', 'High', 'Low', 'Close', 'Volume']]
            
            top_idx = np.argsort(feature_importance)[-10:]
            axes[1, 0].barh(np.array(feature_names)[top_idx], feature_importance[top_idx])
            axes[1, 0].set_title('Top 10 Features Importantes')
        
        # 4. Courbe d'apprentissage LSTM
        if 'LSTM' in self.models:
            history = self.models['LSTM']['history']
            axes[1, 1].plot(history.history['loss'], label='Train Loss')
            axes[1, 1].plot(history.history['val_loss'], label='Validation Loss')
            axes[1, 1].set_title('Courbe d\'apprentissage LSTM')
            axes[1, 1].legend()
        
        plt.tight_layout()
        plt.show()

# UTILISATION
if __name__ == "__main__":
    # Analyse avec Machine Learning
    symbols = ['AAPL', 'TSLA', 'MSFT']
    
    for symbol in symbols:
        try:
            print(f"\n{'#'*60}")
            print(f"ANALYSE ML POUR {symbol}")
            print(f"{'#'*60}")
            
            # Initialisation du détecteur ML
            ml_detector = MLTrendPatternDetector(symbol, period="2y")
            
            # Analyse complète
            ml_detector.comprehensive_analysis()
            
            # Graphiques
            ml_detector.plot_ml_analysis()
            
        except Exception as e:
            print(f"❌ Erreur avec {symbol}: {e}")
            continue

# EXEMPLE DE BACKTESTING AVANCÉ
def backtest_ml_strategy(symbol):
    """Backtest d'une stratégie basée sur les prédictions ML"""
    detector = MLTrendPatternDetector(symbol, period="3y")
    detector.create_technical_features()
    detector.train_models()
    
    df = detector.feature_data.copy()
    X_train, X_test, y_train, y_test, _ = detector.prepare_ml_data()
    
    best_model = detector.models[detector.best_model_name]['model']
    predictions = best_model.predict(X_test)
    
    # Simulation de trading
    df_test = df.iloc[-len(predictions):].copy()
    df_test['Prediction'] = predictions
    df_test['Strategy_Return'] = df_test['Returns'] * df_test['Prediction'].shift(1)
    
    # Performance
    strategy_return = (1 + df_test['Strategy_Return']).prod() - 1
    buy_hold_return = (1 + df_test['Returns']).prod() - 1
    
    print(f"\n📈 BACKTEST STRATÉGIE ML - {symbol}")
    print(f"   Retour Stratégie ML: {strategy_return:.2%}")
    print(f"   Retour Buy & Hold: {buy_hold_return:.2%}")
    print(f"   Excess Return: {strategy_return - buy_hold_return:.2%}")
    
    return strategy_return

# Exécuter le backtesting
backtest_ml_strategy('AAPL')