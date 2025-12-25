from flask import Flask, jsonify
from flask_cors import CORS
import numpy as np
import yfinance as yf
from datetime import datetime, timedelta
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, Input
from tensorflow.keras.optimizers import Adam
from sklearn.preprocessing import MinMaxScaler

app = Flask(__name__)
CORS(app)  # Permet les requêtes depuis Angular[](http://localhost:4200)

# Liste des actifs
ACTIFS = [
     "GLD", "NKE", "TXN", "QCOM", "ORCL", "IBM", "HON", "MMM", "CAT", "BA", "T",
    "CSCO", "INTC", "MRK", "PFE", "CVX", "XOM", "WMT", "KO", "DIS", "PG", "V", "JNJ",
    "JPM", "META", "TSLA", "NVDA", "AMZN", "GOOGL", "MSFT", "AAPL", "CPER", "SLV"
]

# Nombre de jours à prédire
N_FUTURE_DAYS = 5
SEQ_LENGTH = 10  # Nombre de jours utilisés pour la prédiction

def get_next_trading_days(n=5):
    """Retourne les n prochains jours ouvrés (lundi à vendredi)"""
    days = []
    date = datetime.now()
    while len(days) < n:
        date += timedelta(days=1)
        if date.weekday() < 5:  # 0-4 = lundi à vendredi
            days.append(date.strftime('%Y-%m-%d'))
    return days

def predict_for_asset(ticker):
    try:
        # Télécharger les données historiques (2 ans max pour rapidité)
        df = yf.download(ticker, period="6mo", progress=False)
        if df.empty or len(df) < SEQ_LENGTH + 20:
            return {"ticker": ticker, "error": "Pas assez de données historiques"}

        df = df[['Close', 'Volume']].dropna()
        prices = df['Close'].values.reshape(-1, 1)
        volumes = df['Volume'].values.reshape(-1, 1)

        # Normalisation séparée
        price_scaler = MinMaxScaler()
        volume_scaler = MinMaxScaler()
        prices_scaled = price_scaler.fit_transform(prices)
        volumes_scaled = volume_scaler.fit_transform(volumes)

        data_scaled = np.hstack((prices_scaled, volumes_scaled))

        # Créer les séquences
        X = []
        for i in range(len(data_scaled) - SEQ_LENGTH):
            X.append(data_scaled[i:i + SEQ_LENGTH])
        X = np.array(X)

        y = prices_scaled[SEQ_LENGTH:]

        # Modèle LSTM
        model = Sequential([
            Input(shape=(SEQ_LENGTH, 2)),
            LSTM(100, return_sequences=True),
            Dropout(0.3),
            LSTM(50),
            Dropout(0.3),
            Dense(1)
        ])
        model.compile(optimizer=Adam(learning_rate=0.001), loss='mse')
        model.fit(X, y, epochs=8, batch_size=32, verbose=0, validation_split=0.1)

        # Prédictions successives pour les 5 prochains jours
        last_sequence = X[-1:].copy()  # Dernière séquence disponible
        predictions_scaled = []

        for _ in range(N_FUTURE_DAYS):
            pred = model.predict(last_sequence, verbose=0)
            predictions_scaled.append(pred[0, 0])
            # Réinjecter la prédiction (volume estimé à 0 pour simplicité)
            new_row = np.array([[pred[0, 0], 0]])
            last_sequence = np.append(last_sequence[:, 1:, :], [new_row], axis=1)

        # Inverser la normalisation
        predictions_scaled = np.array(predictions_scaled).reshape(-1, 1)
        predictions_real = price_scaler.inverse_transform(predictions_scaled).flatten()

        return {
            "ticker": ticker,
            "predictions": [round(float(p), 2) for p in predictions_real],
            "dates": get_next_trading_days(N_FUTURE_DAYS)
        }

    except Exception as e:
        return {"ticker": ticker, "error": str(e)}

@app.route('/api/predict', methods=['GET'])
def get_predictions():
    """Endpoint principal : renvoie les prédictions pour tous les actifs"""
    print("Début des prédictions pour tous les actifs...")
    results = []
    for ticker in ACTIFS:
        print(f"Prédiction en cours pour {ticker}...")
        pred = predict_for_asset(ticker)
        results.append(pred)

    response = {
        "update_date": datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        "future_dates": get_next_trading_days(N_FUTURE_DAYS),
        "predictions": results
    }
    return jsonify(response)

@app.route('/')
def home():
    return "<h1>API Prédictions Boursières - Flask</h1><p>Utilisez /api/predict pour obtenir les prédictions.</p>"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5002, debug=True)