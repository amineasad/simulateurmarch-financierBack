import json
import ollama

def analyse_sentiment(client, model, texte):
    prompt = f"""
    Analyse le sentiment de cette réclamation.
    Réponds en JSON uniquement !
    
    Texte: "{texte}"
    
    Format attendu :
    {{
        "sentiment": "positif/negatif/neutre",
        "score": 0.80,
        "urgence": "faible/moyenne/elevee"
    }}
    """

    try:
        response = client.generate(model=model, prompt=prompt)
        texte_reponse = response['response']

        if "{" in texte_reponse:
            return json.loads(
                texte_reponse[texte_reponse.find("{"):texte_reponse.rfind("}")+1]
            )

    except Exception as e:
        print("❌ Erreur sentiment :", e)

    return {"sentiment": "neutre", "score": 0.5, "urgence": "moyenne"}
