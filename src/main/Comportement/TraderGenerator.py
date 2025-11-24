# corrected_data_generator.py
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import random

def generate_corrected_trader_data():
    """Génère des données plus réalistes"""
    
    strategies_params = {
        'scalping': {'win_rate': 0.58, 'profit_multiplier': 1.2},
        'day_trading': {'win_rate': 0.55, 'profit_multiplier': 1.1},
        'swing_trading': {'win_rate': 0.52, 'profit_multiplier': 1.3},
        'position_trading': {'win_rate': 0.48, 'profit_multiplier': 1.5},
        'mean_reversion': {'win_rate': 0.60, 'profit_multiplier': 1.1}
    }
    
    trades = []
    for trader_id in range(50):
        strategy = random.choice(list(strategies_params.keys()))
        params = strategies_params[strategy]
        
        # Compétence du trader (varie entre 0.7 et 1.3)
        skill = random.uniform(0.7, 1.3)
        actual_win_rate = min(0.85, params['win_rate'] * skill)
        
        for _ in range(random.randint(200, 600)):  # Trades par trader
            is_win = random.random() < actual_win_rate
            
            # PnL plus réaliste
            if is_win:
                pnl = random.uniform(20, 200) * params['profit_multiplier']
            else:
                pnl = random.uniform(-150, -30)  # Pertes plus petites que gains
            
            trade = {
                'trader_id': f'trader_{trader_id:03d}',
                'strategy': strategy,
                'instrument': random.choice(['AAPL', 'GOOGL', 'MSFT', 'TSLA']),
                'pnl': pnl,
                'is_win': is_win,
                'holding_hours': random.uniform(1, 100),
                'position_size': random.uniform(1000, 50000),
                'risk_reward_ratio': random.uniform(1.5, 3.0),
                'timestamp': datetime.now() - timedelta(days=random.randint(1, 90))
            }
            trades.append(trade)
    
    df = pd.DataFrame(trades)
    df['net_pnl'] = df['pnl'] - df['pnl'] * 0.001  # Frais de 0.1%
    return df

# Générer de nouvelles données corrigées
df_corrected = generate_corrected_trader_data()
df_corrected.to_csv('trader_data_corrected.csv', index=False)