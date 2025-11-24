#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Configuration des patterns pour l'analyse des réclamations financières.
Ce module définit les patterns et règles utilisés par le reclamation-agent.
"""

import re
import json
import logging
from datetime import datetime

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("reclamation_patterns.log"),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger("patternconfig")

# Patterns de reconnaissance pour les réclamations
PATTERNS = {
    "transaction_id": r"TR-\d{6}-[A-Z]{2}",
    "montant": r"\d+(?:[.,]\d{1,2})?\s*(?:€|EUR|USD|\$)",
    "date": r"\d{2}/\d{2}/\d{4}",
    "email": r"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}",
    "telephone": r"(?:\+\d{1,3}\s?)?(?:\(\d{1,4}\)\s?)?\d{1,4}[-.\s]?\d{1,4}[-.\s]?\d{1,9}",
    "compte_bancaire": r"[A-Z]{2}\d{2}[A-Z0-9]{4}\d{7}[A-Z0-9]{0,16}",  # Format IBAN
}

# Catégories de réclamations
CATEGORIES = {
    "transaction_echouee": ["échec", "échoué", "non complétée", "erreur transaction"],
    "frais_contestes": ["frais", "commission", "coût", "tarif", "contestation"],
    "retard_execution": ["retard", "délai", "attente", "lenteur"],
    "erreur_montant": ["montant incorrect", "somme erronée", "erreur de calcul"],
    "probleme_technique": ["technique", "bug", "système", "application", "plateforme"],
    "autre": []  # Catégorie par défaut
}

# Niveaux de priorité
PRIORITES = {
    "haute": ["urgent", "immédiat", "critique", "grave"],
    "moyenne": ["important", "attention", "dès que possible"],
    "basse": []  # Priorité par défaut
}

def load_custom_patterns(file_path=None):
    """
    Charge des patterns personnalisés depuis un fichier JSON.
    
    Args:
        file_path (str): Chemin vers le fichier JSON contenant les patterns personnalisés
        
    Returns:
        dict: Patterns mis à jour
    """
    if not file_path:
        return PATTERNS
        
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            custom_patterns = json.load(f)
            
        # Fusion des patterns par défaut avec les patterns personnalisés
        merged_patterns = {**PATTERNS, **custom_patterns}
        logger.info(f"Patterns personnalisés chargés depuis {file_path}")
        return merged_patterns
    except Exception as e:
        logger.error(f"Erreur lors du chargement des patterns personnalisés: {str(e)}")
        return PATTERNS

def save_patterns(patterns, file_path):
    """
    Sauvegarde les patterns dans un fichier JSON.
    
    Args:
        patterns (dict): Patterns à sauvegarder
        file_path (str): Chemin du fichier de sauvegarde
    """
    try:
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(patterns, f, ensure_ascii=False, indent=4)
        logger.info(f"Patterns sauvegardés dans {file_path}")
        return True
    except Exception as e:
        logger.error(f"Erreur lors de la sauvegarde des patterns: {str(e)}")
        return False

def get_pattern(pattern_name):
    """
    Récupère un pattern spécifique par son nom.
    
    Args:
        pattern_name (str): Nom du pattern à récupérer
        
    Returns:
        str: Expression régulière du pattern
    """
    return PATTERNS.get(pattern_name, None)

def add_pattern(name, pattern):
    """
    Ajoute un nouveau pattern à la configuration.
    
    Args:
        name (str): Nom du pattern
        pattern (str): Expression régulière
        
    Returns:
        bool: True si l'ajout a réussi, False sinon
    """
    try:
        # Vérification de la validité de l'expression régulière
        re.compile(pattern)
        PATTERNS[name] = pattern
        logger.info(f"Pattern '{name}' ajouté avec succès")
        return True
    except re.error:
        logger.error(f"Expression régulière invalide pour le pattern '{name}'")
        return False

def get_category_from_text(text):
    """
    Détermine la catégorie d'une réclamation à partir de son texte.
    
    Args:
        text (str): Texte de la réclamation
        
    Returns:
        str: Catégorie identifiée
    """
    text = text.lower()
    
    for category, keywords in CATEGORIES.items():
        for keyword in keywords:
            if keyword.lower() in text:
                return category
    
    return "autre"

def get_priority_from_text(text):
    """
    Détermine la priorité d'une réclamation à partir de son texte.
    
    Args:
        text (str): Texte de la réclamation
        
    Returns:
        str: Niveau de priorité identifié
    """
    text = text.lower()
    
    for priority, keywords in PRIORITES.items():
        for keyword in keywords:
            if keyword.lower() in text:
                return priority
    
    return "basse"

# Configuration par défaut
DEFAULT_CONFIG = {
    "patterns": PATTERNS,
    "categories": CATEGORIES,
    "priorites": PRIORITES,
    "version": "1.0.0",
    "date_mise_a_jour": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
}

if __name__ == "__main__":
    # Test des fonctions
    print("Patterns disponibles:")
    for name, pattern in PATTERNS.items():
        print(f"  - {name}: {pattern}")
    
    # Exemple d'utilisation
    texte_reclamation = "Bonjour, ma transaction TR-123456-FR du 15/04/2023 d'un montant de 150,50 € n'a pas été exécutée. C'est urgent!"
    categorie = get_category_from_text(texte_reclamation)
    priorite = get_priority_from_text(texte_reclamation)
    
    print(f"\nCatégorie détectée: {categorie}")
    print(f"Priorité détectée: {priorite}")