import ollama
import json
from config.prompts import get_prompt

class Categoriseur:
    def __init__(self, model_name="llama2"):
        self.client = ollama.Client()
        self.model = model_name
    
    def categoriser(self, texte):
        prompt_config = get_prompt('categorisation')
        prompt = prompt_config['user_template'].format(texte=texte)
        
        try:
            response = self.client.generate(
                model=self.model,
                prompt=prompt,
                system=prompt_config['system_prompt']
            )
            return json.loads(response['response'])
        except Exception as e:
            print(f"Erreur catégorisation: {e}")
            return {
                "categorie": "service", 
                "sous_categorie": "general", 
                "priorite": 2
            }