import json
from datetime import datetime
import ollama

class AgentReclamationsTrading:
    def __init__(self):
        self.client = ollama.Client()
        self.model = "llama2"
    
    def analyser_sentiment(self, texte):
        """Analyse simplifiée du sentiment"""
        prompt = f"""
        Analyse le sentiment de cette réclamation sur une plateforme de trading simulé.
        Réponds UNIQUEMENT en JSON sans commentaires.
        
        Texte: "{texte}"
        
        Format JSON requis:
        {{
            "sentiment": "positif/negatif/neutre",
            "score": 0.85,
            "emotion_principale": "frustration",
            "urgence": "faible/moyenne/élevée"
        }}
        """
        
        try:
            response = self.client.generate(model=self.model, prompt=prompt)
            print("📨 Réponse Ollama:", response['response'][:100] + "...")
            return json.loads(response['response'])
        except Exception as e:
            print(f"❌ Erreur analyse sentiment: {e}")
            return {"sentiment": "neutre", "score": 0.5, "emotion_principale": "inconnue", "urgence": "moyenne"}
    
    def categoriser(self, texte):
        """Catégorisation simplifiée"""
        prompt = f"""
        Catégorise cette réclamation de trading simulé.
        Réponds UNIQUEMENT en JSON sans commentaires.
        
        Texte: "{texte}"
        
        Catégories: technique, simulation, compte, fonctionnalite, performance
        
        Format JSON requis:
        {{
            "categorie": "technique",
            "sous_categorie": "bug_interface", 
            "priorite": 3
        }}
        """
        
        try:
            response = self.client.generate(model=self.model, prompt=prompt)
            return json.loads(response['response'])
        except Exception as e:
            print(f"❌ Erreur catégorisation: {e}")
            return {"categorie": "technique", "sous_categorie": "general", "priorite": 2}
    
    def generer_reponse(self, texte, analyse):
        """Génération de réponse simplifiée"""
        prompt = f"""
        Tu es un agent de support pour une plateforme de trading simulé.
        Génère une réponse professionnelle et empathique.
        
        RÉCLAMATION: "{texte}"
        
        Ta réponse doit:
        - Reconnaître le problème
        - Être empathique
        - Proposer une solution ou investigation
        - Maximum 3 phrases
        """
        
        try:
            response = self.client.generate(model=self.model, prompt=prompt)
            return response['response']
        except Exception as e:
            print(f"❌ Erreur génération réponse: {e}")
            return "Nous traitons votre réclamation concernant notre plateforme de trading simulé."
    
    def traiter_reclamation_trading(self, texte):
        print("🔍 Analyse réclamation trading...")
        
        # Analyses
        sentiment = self.analyser_sentiment(texte)
        print("✅ Sentiment analysé")
        
        categorie = self.categoriser(texte)
        print("✅ Catégorisation terminée")
        
        # Génération réponse
        reponse = self.generer_reponse(texte, {
            "sentiment": sentiment,
            "categorie": categorie
        })
        
        resultat = {
            "id": datetime.now().strftime("%Y%m%d_%H%M%S"),
            "reclamation": texte,
            "analyse_sentiment": sentiment,
            "categorisation": categorie,
            "reponse_agent": reponse,
            "date_traitement": datetime.now().isoformat()
        }
        
        print("✅ Analyse terminée avec succès")
        return resultat

# Test simplifié
if __name__ == "__main__":
    agent = AgentReclamationsTrading()
    
    test_reclamation = "La plateforme a crashé pendant mon backtest, toute ma stratégie est perdue !"
    
    print("🧪 TEST AGENT RECLAMATIONS TRADING")
    print("=" * 50)
    print(f"Réclamation: {test_reclamation}")
    print("=" * 50)
    
    try:
        resultat = agent.traiter_reclamation_trading(test_reclamation)
        
        print("\n📊 RÉSULTATS:")
        print(f"Sentiment: {resultat['analyse_sentiment']['sentiment']}")
        print(f"Catégorie: {resultat['categorisation']['categorie']}")
        print(f"Priorité: {resultat['categorisation']['priorite']}")
        print(f"💬 Réponse: {resultat['reponse_agent']}")
        
    except Exception as e:
        print(f"❌ Erreur générale: {e}")
        print("💡 Conseil: Vérifie qu'Ollama est bien installé et que le modèle llama2 est téléchargé")