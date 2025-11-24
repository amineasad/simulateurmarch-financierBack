// Données de démonstration pour l'interface
const mockData = {
    strategies: [
        {
            name: 'Momentum',
            classification: 'GAGNANTE',
            winRate: 58.2,
            profitFactor: 1.75,
            pnlTotal: 5240
        },
        {
            name: 'Mean Reversion',
            classification: 'NEUTRE',
            winRate: 48.7,
            profitFactor: 1.12,
            pnlTotal: 1850
        },
        {
            name: 'Breakout',
            classification: 'À RISQUE',
            winRate: 42.1,
            profitFactor: 0.95,
            pnlTotal: -320
        },
        {
            name: 'Trend Following',
            classification: 'PERDANTE',
            winRate: 35.6,
            profitFactor: 0.68,
            pnlTotal: -1450
        }
    ]
};



// Fonction pour mettre à jour le tableau des stratégies
function updateStrategyTable(strategies) {
    const tableBody = document.getElementById('strategy-table-body');
    tableBody.innerHTML = '';
    
    strategies.forEach(strategy => {
        const row = document.createElement('tr');
        
        // Déterminer la classe CSS pour le badge
        let badgeClass = '';
        switch(strategy.classification) {
            case 'GAGNANTE':
                badgeClass = 'success';
                break;
            case 'NEUTRE':
                badgeClass = 'neutral';
                break;
            case 'À RISQUE':
                badgeClass = 'warning';
                break;
            case 'PERDANTE':
                badgeClass = 'danger';
                break;
            default:
                badgeClass = 'neutral';
        }
        
        // Formater le PnL avec le signe € et les séparateurs de milliers
        const formattedPnl = new Intl.NumberFormat('fr-FR', {
            style: 'currency',
            currency: 'EUR',
            maximumFractionDigits: 0
        }).format(strategy.pnlTotal);
        
        row.innerHTML = `
            <td>${strategy.name}</td>
            <td><span class="badge ${badgeClass}">${strategy.classification}</span></td>
            <td>${strategy.winRate.toFixed(1)}%</td>
            <td>${strategy.profitFactor.toFixed(2)}</td>
            <td>${formattedPnl}</td>
            <td>
                <button class="btn-icon" onclick="viewStrategyDetails('${strategy.name}')">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M12 5V19" stroke="#1E88E5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M5 12H19" stroke="#1E88E5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </button>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Fonction pour afficher les détails d'une stratégie (à implémenter)
function viewStrategyDetails(strategyName) {
    alert(`Détails de la stratégie ${strategyName} - Fonctionnalité à venir`);
}

// Fonction pour exécuter l'analyse des traders
async function runTraderAnalysis() {
    try {
        // Simuler un appel API pour exécuter l'analyse
        // En production, remplacer par un appel API réel
        // const response = await fetch('/api/run-trader-analysis', { method: 'POST' });
        // const result = await response.json();
        
        // Simuler un délai de traitement
        await new Promise(resolve => setTimeout(resolve, 1500));
        
        // Recharger les données
        loadTraderAnalysisData();
        
        // Afficher un message de succès
        alert('Analyse des traders exécutée avec succès');
    } catch (error) {
        console.error('Erreur lors de l\'exécution de l\'analyse:', error);
        alert('Erreur lors de l\'exécution de l\'analyse. Veuillez réessayer.');
    }
}

// Fonction pour gérer la navigation
function setupNavigation() {
    const navLinks = document.querySelectorAll('nav a');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetSection = e.target.getAttribute('data-section');
            showSection(targetSection);
        });
    });
}

function showSection(sectionName) {
    const sections = document.querySelectorAll('.section');
    sections.forEach(sec => sec.classList.remove('active'));
    const targetSection = document.querySelector(`[data-section="${sectionName}"]`);
    targetSection.classList.add('active');

    const navLinks = document.querySelectorAll('nav a');
    navLinks.forEach(lnk => lnk.classList.remove('active'));
    const targetLink = document.querySelector(`nav a[data-section="${sectionName}"]`);
    targetLink.classList.add('active');
}

// Fonction pour mettre à jour les graphiques
function renderCharts(strategies) {
    // Bar chart
    const barChart = document.querySelector('.bar-chart');
    barChart.innerHTML = '';
    const maxPnL = Math.max(...strategies.map(s => Math.abs(s.pnlTotal)));
    strategies.forEach(strat => {
        const percentage = (Math.abs(strat.pnlTotal) / maxPnL) * 80; // scale to 80% max
        const barDiv = document.createElement('div');
        barDiv.className = 'bar-container';
        const formattedPnl = strat.pnlTotal >= 0 ? '€' + strat.pnlTotal : '-€' + Math.abs(strat.pnlTotal);
        barDiv.innerHTML = `
            <div class="bar" style="height: ${percentage}%;" data-value="${formattedPnl}"></div>
            <div class="bar-label">${strat.name}</div>
        `;
        barChart.appendChild(barDiv);
    });

    // Pie chart: update based on classifications
    // Simple update: count winning vs losing
    const winning = strategies.filter(s => s.classification === 'GAGNANTE').length;
    const total = strategies.length;
    const winPercent = (winning / total) * 100;
    const pieChart = document.querySelector('.pie-chart');
    pieChart.style.background = `conic-gradient(
        var(--success-color) 0% ${winPercent}%, 
        var(--danger-color) ${winPercent}% 100%
    )`;
}

// Initialisation de l'application
document.addEventListener('DOMContentLoaded', () => {
    // Configurer la navigation
    setupNavigation();
    
    // Charger les données initiales
    loadTraderAnalysisData();
    
    // Ajouter un écouteur d'événement pour le bouton d'actualisation
    const refreshButton = document.getElementById('refresh-btn');
    if (refreshButton) {
        refreshButton.addEventListener('click', runTraderAnalysis);
    }
});

// Fonction pour charger les données depuis le backend
async function loadTraderAnalysisData() {
    try {
        // En production, remplacer par un appel API réel
        // const response = await fetch('/api/trader-analysis');
        // const data = await response.json();
        
        // Pour la démonstration, utiliser les données mockées
        const data = { strategies: mockData.strategies };
        
        // Mettre à jour le tableau des stratégies
        updateStrategyTable(data.strategies);
        
        // Mettre à jour les graphiques
        renderCharts(data.strategies);
        
        // Afficher un message de succès
        console.log('Données chargées avec succès');
    } catch (error) {
        console.error('Erreur lors du chargement des données:', error);
    }
}

// Fonction pour créer une connexion avec le backend Python
async function connectToBackend() {
    try {
        // En production, remplacer par l'URL réelle de l'API
        const apiUrl = '/api/trader-analysis';
        
        // Vérifier la connexion
        const response = await fetch(apiUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            console.log('Connexion au backend établie avec succès');
            return true;
        } else {
            console.error('Erreur de connexion au backend:', response.statusText);
            return false;
        }
    } catch (error) {
        console.error('Erreur lors de la tentative de connexion au backend:', error);
        return false;
    }
}