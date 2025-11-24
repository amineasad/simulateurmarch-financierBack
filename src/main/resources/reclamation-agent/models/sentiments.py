import ollama
import json
from config.prompts import get_prompt

class AnalyseurSentiment:
    def __init__(self, model_name="llama2"):
        self.client = ollama.Client()
        self.model = model_name
    
    def analyser(self, texte):
        prompt_config = get_prompt('sentiment')
        prompt = prompt_config['user_template'].format(texte=texte)
        
        try:
            response = self.client.generate(
                model=self.model,
                prompt=prompt,
                system=prompt_config['system_prompt']
            )
            return json.loads(response['response'])
        except Exception as e:
            print(f"Erreur analyse sentiment: {e}")
            return {
                "sentiment": "neutre", 
                "score": 0.5, 
                "emotion_principale": "inconnue", 
                "urgence": "moyenne"
            }