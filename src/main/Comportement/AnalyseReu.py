# service_analyse_avec_test.py
import pandas as pd
import numpy as np
from datetime import datetime

class ServiceAnalyseTrading:
    def __init__(self, data_path):
        self.df = pd.read_csv(data_path)
        self.seuils = {
            'excellente': {'win_rate': 0.55, 'profit_factor': 1.5},
            'bonne': {'win_rate': 0.52, 'profit_factor': 1.2},
            'moyenne': {'win_rate': 0.48, 'profit_factor': 1.0},
            'risquee': {'win_rate': 0.42, 'profit_factor': 0.8}
        }
        print(f"✅ Service initialisé avec {len(self.df):,} trades")
    
    def analyser_trader(self, trader_id):
        """Analyse complète d'un trader spécifique"""
        trades = self.df[self.df['trader_id'] == trader_id]
        
        if len(trades) < 5:
            return self._resultat_insuffisant()
        
        return self._calculer_analyse(trades, trader_id)
    
    def analyser_strategie(self, strategy):
        """Analyse d'une stratégie spécifique"""
        trades = self.df[self.df['strategy'] == strategy]
        return self._calculer_analyse_strategie(trades, strategy)
    
    def get_top_traders(self, n=10):
        """Retourne les meilleurs traders"""
        perf_traders = self.df.groupby('trader_id').agg({
            'net_pnl': 'sum',
            'is_win': 'mean',
            'strategy': lambda x: x.mode()[0]
        }).round(3)
        
        return perf_traders.nlargest(n, 'net_pnl')
    
    def _calculer_analyse(self, trades, trader_id):
        """Calcule l'analyse détaillée"""
        win_rate = trades['is_win'].mean()
        pnl_total = trades['net_pnl'].sum()
        pnl_moyen = trades['net_pnl'].mean()
        
        # Calcul métriques avancées
        gains = trades[trades['net_pnl'] > 0]['net_pnl']
        pertes = trades[trades['net_pnl'] < 0]['net_pnl']
        
        profit_factor = gains.sum() / abs(pertes.sum()) if len(pertes) > 0 else float('inf')
        avg_win = gains.mean() if len(gains) > 0 else 0
        avg_loss = pertes.mean() if len(pertes) > 0 else 0
        rr_ratio = abs(avg_win / avg_loss) if avg_loss != 0 else 0
        
        # Classification
        statut, conseil = self._classifier_performance(win_rate, pnl_total, profit_factor)
        
        return {
            'trader_id': trader_id,
            'statut': statut,
            'win_rate': round(win_rate, 3),
            'pnl_total': round(pnl_total, 2),
            'pnl_moyen': round(pnl_moyen, 2),
            'profit_factor': round(profit_factor, 2),
            'ratio_risque_reward': round(rr_ratio, 2),
            'nombre_trades': len(trades),
            'strategie_principale': trades['strategy'].mode()[0],
            'conseil': conseil,
            'timestamp_analyse': datetime.now().isoformat()
        }
    
    def _classifier_performance(self, win_rate, pnl_total, profit_factor):
        """Classifie la performance du trader"""
        if win_rate > self.seuils['excellente']['win_rate'] and profit_factor > self.seuils['excellente']['profit_factor']:
            return "🟢 EXCELLENTE", "Performance exceptionnelle! Continuez sur cette voie."
        elif win_rate > self.seuils['bonne']['win_rate'] and profit_factor > self.seuils['bonne']['profit_factor']:
            return "🟢 BONNE", "Très bonne performance. Travaillez à réduire vos pertes moyennes."
        elif win_rate > self.seuils['moyenne']['win_rate'] and pnl_total > 0:
            return "🟡 MOYENNE", "Performance correcte. Concentrez-vous sur la consistance."
        elif win_rate > self.seuils['risquee']['win_rate']:
            return "🟠 RISQUÉE", "Performance fragile. Revoir la gestion des risques."
        else:
            return "🔴 CRITIQUE", "Stratégie non rentable. Formation recommandée."
    
    def _calculer_analyse_strategie(self, trades, strategy):
        """Analyse détaillée d'une stratégie"""
        win_rate = trades['is_win'].mean()
        pnl_total = trades['net_pnl'].sum()
        n_traders = trades['trader_id'].nunique()
        
        return {
            'strategie': strategy,
            'win_rate': round(win_rate, 3),
            'pnl_total': round(pnl_total, 2),
            'trades_analyses': len(trades),
            'traders_utilisant': n_traders,
            'performance_moyenne': 'Haute' if win_rate > 0.52 else 'Moyenne' if win_rate > 0.48 else 'Basse'
        }
    
    def _resultat_insuffisant(self):
        return {
            'statut': "DONNÉES INSUFFISANTES",
            'conseil': "Au moins 5 trades sont nécessaires pour une analyse fiable.",
            'nombre_trades': 0
        }

# ==================== PARTIE TEST AUTOMATIQUE ====================
def tester_service():
    """Test automatique du service"""
    print("\n" + "="*60)
    print("🧪 TEST AUTOMATIQUE DU SERVICE D'ANALYSE")
    print("="*60)
    
    # Initialisation
    service = ServiceAnalyseTrading(
        r"C:\Users\M A\OneDrive\Documents\PIDEV\simulateurmarch-financierBack\Comportement\trader_data_corrected.csv"
    )
    
    # Test 1: Analyse de quelques traders
    print("\n📊 TEST 1: ANALYSE TRADERS INDIVIDUELS")
    print("-" * 40)
    
    traders_test = ['trader_001', 'trader_042', 'trader_999']
    
    for trader_id in traders_test:
        resultat = service.analyser_trader(trader_id)
        print(f"\n🎯 {trader_id}:")
        print(f"   Statut: {resultat['statut']}")
        print(f"   Win Rate: {resultat['win_rate']:.1%}")
        print(f"   PnL Total: ${resultat['pnl_total']:,.2f}")
        print(f"   Profit Factor: {resultat['profit_factor']:.2f}")
        print(f"   Trades: {resultat['nombre_trades']}")
        if 'conseil' in resultat:
            print(f"   Conseil: {resultat['conseil']}")
    
    # Test 2: Top traders
    print(f"\n🏆 TEST 2: TOP 5 TRADERS")
    print("-" * 40)
    
    top_traders = service.get_top_traders(5)
    for i, (trader_id, row) in enumerate(top_traders.iterrows(), 1):
        print(f"   {i}. {trader_id} | {row['strategy']:15} | PnL: ${row['net_pnl']:>8,.0f} | WR: {row['is_win']:.1%}")
    
    # Test 3: Analyse stratégies
    print(f"\n🎯 TEST 3: ANALYSE STRATÉGIES")
    print("-" * 40)
    
    for strategie in service.df['strategy'].unique():
        resultat = service.analyser_strategie(strategie)
        print(f"   • {strategie:15} | WR: {resultat['win_rate']:>5.1%} | PnL: ${resultat['pnl_total']:>8,.0f} | {resultat['performance_moyenne']}")
    
    # Test 4: Stats globales
    print(f"\n📈 TEST 4: STATISTIQUES GLOBALES")
    print("-" * 40)
    
    win_rate_global = service.df['is_win'].mean()
    pnl_total_global = service.df['net_pnl'].sum()
    
    print(f"   Win Rate Global: {win_rate_global:.1%}")
    print(f"   PnL Total: ${pnl_total_global:,.2f}")
    print(f"   Trades Analysés: {len(service.df):,}")
    print(f"   Traders Uniques: {service.df['trader_id'].nunique()}")
    
    print(f"\n✅ TEST TERMINÉ AVEC SUCCÈS!")
    return service

# Exécution automatique si le fichier est lancé directement
if __name__ == "__main__":
    service = tester_service()