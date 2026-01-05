from flask import Flask, request, jsonify
from datetime import datetime
from models.sentiments import analyse_sentiment
from models.categorization import categoriser
from models.response_generator import generer_reponse
import ollama

app = Flask(__name__)
client = ollama.Client()
MODEL_NAME = "llama2:7b"


@app.route("/analyser", methods=["POST"])
def analyser():
    data = request.json
    texte = data.get("texte")

    if not texte:
        return jsonify({"success": False, "error": "texte manquant"}), 400

    try:
        sentiment = analyse_sentiment(client, MODEL_NAME, texte)
        categorie = categoriser(client, MODEL_NAME, texte)
        reponse = generer_reponse(client, MODEL_NAME, texte)

        resultat = {
            "success": True,
            "timestamp": datetime.now().isoformat(),
            "texte": texte,
            "sentiment": sentiment,
            "categorie": categorie,
            "reponse": reponse
        }
        return jsonify(resultat)

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
