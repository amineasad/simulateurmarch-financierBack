import json
import ollama

def categoriser(client, model, texte):
    prompt = f"""
    Catégorise cette réclamation liée à une plateforme de trading simulé.
    Réponds uniquement en JSON valide.
    
    Texte: "{texte}"
    
    Format attendu :
    {{
        "categorie": "technique/simulation/compte/performance",
        "priorite": 1-4
    }}
    """

    try:
        response = client.generate(model=model, prompt=prompt)
        texte_reponse = response['response']

        if "{" in texte_reponse:
            json_clean = texte_reponse[
                         texte_reponse.find("{"):texte_reponse.rfind("}")+1
                         ]
            return json.loads(json_clean)

    except Exception as e:
        print("❌ Erreur catégorisation :", e)

    return {"categorie": "general", "priorite": 3}
