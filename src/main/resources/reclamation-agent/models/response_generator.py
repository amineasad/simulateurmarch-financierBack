import ollama
from config.prompts import get_prompt

class GenerateurReponse:
    def __init__(self, model_name="llama2"):
        self.client = ollama.Client()
        self.model = model_name
    
    def generer(self, texte, analyse):
        prompt_config = get_prompt('reponse')
        prompt = prompt_config['user_template'].format(
            texte=texte, 
            analyse=analyse
        )
        
        try:
            response = self.client.generate(
                model=self.model,
                prompt=prompt,
                system=prompt_config['system_prompt']
            )
            return response['response']
        except Exception as e:
            print(f"Erreur génération réponse: {e}")
            return "Nous avons bien reçu votre réclamation et la traitons dans les plus brefs délais."