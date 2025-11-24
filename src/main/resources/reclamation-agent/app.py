import ollama
import json
from datetime import datetime

class AgentReclamationsTrading:
    def __init__(self):
        self.client = ollama.Client()
        self.model = "llama2:7b"  # Utiliser le modèle exact
    
    def verifier_ollama(self):
        """Vérifie qu'Ollama fonctionne"""
        try:
            response = self.client.generate(
                model=self.model,
                prompt="Réponds juste 'OK'",
                options={'temperature': 0.1}
            )
            return True
        except Exception as e:
            print(f"❌ Erreur Ollama: {e}")
            return False
    
    def analyser_sentiment(self, texte):
        """Analyse du sentiment - version simplifiée"""
        prompt = f"""
        Analyse cette réclamation et retourne UNIQUEMENT du JSON.
        
        Texte: "{texte}"
        
        Réponds exactement dans ce format JSON:
        {{
            "sentiment": "negatif",
            "score": 0.8,
            "urgence": "elevee"
        }}
        """
        
        try:
            response = self.client.generate(
                model=self.model,
                prompt=prompt,
                options={'temperature': 0.1}
            )
            reponse = response['response'].strip()
            print(f"📨 Réponse sentiment: {reponse}")
            
            # Nettoyer la réponse pour obtenir du JSON valide
            if "{" in reponse:
                json_str = reponse[reponse.find("{"):reponse.rfind("}")+1]
                return json.loads(json_str)
            else:
                # Si pas de JSON, analyser manuellement
                if "negatif" in reponse.lower():
                    return {"sentiment": "negatif", "score": 0.8, "urgence": "elevee"}
                elif "positif" in reponse.lower():
                    return {"sentiment": "positif", "score": 0.6, "urgence": "faible"}
                else:
                    return {"sentiment": "neutre", "score": 0.5, "urgence": "moyenne"}
                    
        except Exception as e:
            print(f"❌ Erreur analyse sentiment: {e}")
            return {"sentiment": "neutre", "score": 0.5, "urgence": "moyenne"}
    
    def categoriser(self, texte):
        """Catégorisation simplifiée"""
        prompt = f"""
        Catégorise cette réclamation de trading simulé.
        
        Texte: "{texte}"
        
        Réponds UNIQUEMENT en JSON:
        {{
            "categorie": "technique",
            "priorite": 3
        }}
        """
        
        try:
            response = self.client.generate(
                model=self.model,
                prompt=prompt,
                options={'temperature': 0.1}
            )
            reponse = response['response'].strip()
            print(f"📨 Réponse catégorisation: {reponse}")
            
            if "{" in reponse:
                json_str = reponse[reponse.find("{"):reponse.rfind("}")+1]
                return json.loads(json_str)
            else:
                # Catégorisation manuelle basée sur les mots-clés
                texte_lower = texte.lower()
                if any(mot in texte_lower for mot in ['crash', 'bug', 'planté', 'lent']):
                    return {"categorie": "technique", "priorite": 4}
                elif any(mot in texte_lower for mot in ['prix', 'simulation', 'réalist']):
                    return {"categorie": "simulation", "priorite": 3}
                elif any(mot in texte_lower for mot in ['compte', 'solde', 'historique']):
                    return {"categorie": "compte", "priorite": 2}
                else:
                    return {"categorie": "general", "priorite": 2}
                    
        except Exception as e:
            print(f"❌ Erreur catégorisation: {e}")
            return {"categorie": "technique", "priorite": 2}
    
    def generer_reponse(self, texte, analyse):
        """Génère une réponse personnalisée"""
        prompt = f"""
        Tu es un agent de support pour une plateforme de trading simulé appelée "Marché Simulé".
        
        RÉCLAMATION: "{texte}"
        
        ANALYSE: {analyse}
        
        Génère une réponse professionnelle et empathique (2-3 phrases):
        - Commence par t'excuser
        - Propose une solution ou investigation
        - Donne de l'espoir
        - Sois concret
        """
        
        try:
            response = self.client.generate(
                model=self.model,
                prompt=prompt,
                options={'temperature': 0.7}
            )
            return response['response'].strip()
        except Exception as e:
            print(f"❌ Erreur génération réponse: {e}")
            return "Nous avons bien reçu votre réclamation et notre équipe technique l'analyse dans les plus brefs délais."
    
    def traiter_reclamation(self, texte):
        """Traite une réclamation complètement"""
        print(f"📝 Réclamation: {texte}")
        
        if not self.verifier_ollama():
            print("❌ Ollama n'est pas prêt")
            return None
        
        print("🔄 Analyse en cours...")
        
        # Analyses séquentielles
        print("1. Analyse du sentiment...")
        sentiment = self.analyser_sentiment(texte)
        
        print("2. Catégorisation...")
        categorie = self.categoriser(texte)
        
        print("3. Génération de réponse...")
        reponse = self.generer_reponse(texte, {
            "sentiment": sentiment,
            "categorie": categorie
        })
        
        # Résultat final
        resultat = {
            "reclamation": texte,
            "analyse_sentiment": sentiment,
            "categorisation": categorie,
            "reponse_agent": reponse,
            "date_traitement": datetime.now().isoformat()
        }
        
        print("✅ Analyse terminée avec succès!")
        return resultat

def main():
    print("🚀 AGENT RECLAMATIONS - MARCHÉ SIMULÉ")
    print("=" * 60)
    print("📦 Modèle détecté: llama2:7b")
    print("=" * 60)
    
    # Créer l'agent
    agent = AgentReclamationsTrading()
    
    # Test de connexion
    if not agent.verifier_ollama():
        print("❌ Impossible de se connecter à Ollama")
        print("💡 Vérifiez que 'ollama serve' est lancé")
        return
    
    print("✅ Connexion Ollama réussie!")
    
    # Tests avec des réclamations typiques
    tests = [
        "La plateforme a crashé pendant mon backtest, toute ma stratégie est perdue !",
        "Les prix simulés ne sont pas réalistes, ça fausse mon apprentissage du trading.",
        "Je n'arrive pas à accéder à mon historique de trades, c'est très frustrant.",
        "L'interface est trop lente pendant les heures de trading actif, je rate des opportunités."
    ]
    
    for i, texte in enumerate(tests, 1):
        print(f"\n🧪 TEST {i}/4")
        print('-' * 50)
        
        resultat = agent.traiter_reclamation(texte)
        
        if resultat:
            print(f"\n📊 RÉSULTATS:")
            print(f"🎭 Sentiment: {resultat['analyse_sentiment']}")
            print(f"🏷️ Catégorie: {resultat['categorisation']}")
            print(f"💬 Réponse: {resultat['reponse_agent']}")
        else:
            print("❌ Échec de l'analyse")
        
        print('-' * 50)

if __name__ == "__main__":
    main()