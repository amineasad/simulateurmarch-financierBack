import ollama
import json
from config.prompts import get_prompt

class AnalyseurPlateforme:
    def __init__(self, model_name="llama2"):
        self.client = ollama.Client()
        self.model = model_name
    
    def detecter_theme_trading(self, texte):
        """Détecte les thèmes spécifiques au trading"""
        prompt = f"""
        Analyse ce message concernant une plateforme de trading simulé et identifie les thèmes:
        "{texte}"
        
        Thèmes possibles: technique, simulation, apprentissage, interface, performance, données
        Retourne en JSON:
        {{
            "themes": ["technique", "performance"],
            "mots_cles": ["crash", "stratégie", "ordres"],
            "niveau_technicite": "debutant/intermediaire/expert"
        }}
        """
        
        try:
            response = self.client.generate(model=self.model, prompt=prompt)
            return json.loads(response['response'])
        except:
            return {"themes": ["general"], "mots_cles": [], "niveau_technicite": "debutant"}