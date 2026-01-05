def generer_reponse(client, model, texte):
    prompt = f"""
    Tu es un support technique d'une plateforme de trading simulé.
    RÉCLAMATION : "{texte}"

    Réponds avec 2 à 3 phrases :
    - Empathique
    - Professionnelle
    - Proposition d'investigation ou solution
    """

    try:
        response = client.generate(model=model, prompt=prompt)
        return response['response'].strip()

    except Exception as e:
        print("❌ Erreur génération réponse :", e)
        return "Merci pour votre message, notre équipe technique analyse le problème."
