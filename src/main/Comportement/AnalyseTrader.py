# robust_analysis.py
import pandas as pd
import numpy as np

class AnalyseurRobuste:
    def __init__(self):
        self.seuils = {
            'gagnante': {'win_rate': 0.52, 'profit_factor': 1.2},
            'neutre': {'win_rate': 0.45, 'profit_factor': 1.0},
            'a_risque': {'win_rate': 0.40, 'profit_factor': 0.8}
        }
    
    def analyser_strategie(self, df_strategy):
        """Analyse robuste d'une stratégie"""
        if len(df_strategy) < 10:
            return "INSUFFISANT_DATA"
        
        win_rate = df_strategy['is_win'].mean()
        total_gains = df_strategy[df_strategy['net_pnl'] > 0]['net_pnl'].sum()
        total_pertes = abs(df_strategy[df_strategy['net_pnl'] < 0]['net_pnl'].sum())
        
        # Éviter division par zéro
        if total_pertes == 0:
            profit_factor = float('inf')
        else:
            profit_factor = total_gains / total_pertes
        
        # Classification
        if win_rate >= self.seuils['gagnante']['win_rate'] and profit_factor >= self.seuils['gagnante']['profit_factor']:
            return "GAGNANTE"
        elif win_rate >= self.seuils['neutre']['win_rate'] and profit_factor >= self.seuils['neutre']['profit_factor']:
            return "NEUTRE"
        elif win_rate >= self.seuils['a_risque']['win_rate']:
            return "À RISQUE"
        else:
            return "PERDANTE"
    
    def analyser_dataset_complet(self, df):
        """Analyse complète du dataset"""
        print("🎯 ANALYSE ROBUSTE DES STRATÉGIES")
        print("="*50)
        
        strategies = df['strategy'].unique()
        
        for strategy in strategies:
            df_strat = df[df['strategy'] == strategy]
            classification = self.analyser_strategie(df_strat)
            
            win_rate = df_strat['is_win'].mean()
            pnl_total = df_strat['net_pnl'].sum()
            n_trades = len(df_strat)
            
            print(f"• {strategy:15} | {classification:^10} | WR: {win_rate:.1%} | PnL: ${pnl_total:>8,.0f} | Trades: {n_trades:>4}")

# Utilisation
analyseur = AnalyseurRobuste()
df = pd.read_csv(r'C:\Users\M A\OneDrive\Documents\PIDEV\simulateurmarch-financierBack\Comportement\trader_data_corrected.csv')  # Ou vos nouvelles données
analyseur.analyser_dataset_complet(df)