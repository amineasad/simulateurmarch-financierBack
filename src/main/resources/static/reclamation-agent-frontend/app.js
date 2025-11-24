// Fonction pour analyser une réclamation
async function analyzeReclamation() {
    const text = document.getElementById('reclamation-text').value.trim();
    if (!text) {
        alert('Veuillez entrer un texte de réclamation.');
        return;
    }

    // Reset results
    document.getElementById('category-result').textContent = 'Analysing...';
    document.getElementById('sentiment-result').textContent = 'Analysing...';
    document.getElementById('response-result').textContent = 'Analysing...';

    try {
        const response = await fetch('/examen/api/reclamations/analyser', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ texte: text }),
        });

        if (!response.ok) {
            throw new Error('Erreur lors de l\'analyse.');
        }

        const result = await response.json();

        // Afficher les résultats (adapter selon la structure de retour)
        document.getElementById('category-result').textContent = result.categorie || 'Non défini';
        document.getElementById('sentiment-result').textContent = result.priorite || 'Non défini';
        document.getElementById('response-result').textContent = result.reponse_suggeree || 'Non définie';
    } catch (error) {
        console.error('Erreur:', error);
        document.getElementById('category-result').textContent = 'Erreur';
        document.getElementById('sentiment-result').textContent = 'Erreur';
        document.getElementById('response-result').textContent = 'Erreur';
        alert('Erreur lors de l\'analyse. Vérifiez la connexion.');
    }
}

// Fonction pour analyser un lot de réclamations
async function analyzeBatch() {
    const text = document.getElementById('batch-text').value.trim();
    if (!text) {
        alert('Veuillez entrer des réclamations.');
        return;
    }

    const reclamations = text.split('\n').filter(line => line.trim() !== '');
    if (reclamations.length === 0) {
        alert('Aucune réclamation valide.');
        return;
    }

    const tbody = document.querySelector('#batch-results-table tbody');
    tbody.innerHTML = '<tr><td colspan="4">Analysing...</td></tr>';

    try {
        const response = await fetch('/examen/api/reclamations/traiter-lot', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ reclamations }),
        });

        if (!response.ok) {
            throw new Error('Erreur lors du traitement du lot.');
        }

        const results = await response.json();
        tbody.innerHTML = '';

        results.forEach((result, index) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${reclamations[index]}</td>
                <td>${result.categorie || '-'}</td>
                <td>${result.priorite || '-'}</td>
                <td>${result.reponse_suggeree || '-'}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Erreur:', error);
        tbody.innerHTML = '<tr><td colspan="4">Erreur lors du traitement.</td></tr>';
        alert('Erreur lors de l\'analyse du lot. Vérifiez la connexion.');
    }
}

// Initialisation des événements
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('analyze-btn').addEventListener('click', analyzeReclamation);
    document.getElementById('batch-analyze-btn').addEventListener('click', analyzeBatch);
});
