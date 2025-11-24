#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Agent de traitement des réclamations financières.
Ce module analyse et traite les réclamations des clients du marché financier.
"""

import os
import re
import json
import logging
import argparse
from datetime import datetime
import patternconfig as pc

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("reclamation_agent.log"),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger("reclamation-agent")

class ReclamationAgent:
    """Agent de traitement des réclamations financières."""
    
    def __init__(self, config_file=None):
        """
        Initialise l'agent de traitement des réclamations.
        
        Args:
            config_file (str, optional): Chemin vers un fichier de configuration personnalisé
        """
        self.patterns = pc.load_custom_patterns(config_file)
        self.categories = pc.CATEGORIES
        self.priorites = pc.PRIORITES
        logger.info("Agent de réclamation initialisé")
        
    def extraire_informations(self, texte):
        """
        Extrait les informations pertinentes d'une réclamation.
        
        Args:
            texte (str): Texte de la réclamation
            
        Returns:
            dict: Informations extraites
        """
        informations = {}
        
        # Extraction des informations à l'aide des patterns
        for nom_pattern, pattern in self.patterns.items():
            match = re.search(pattern, texte)
            if match:
                informations[nom_pattern] = match.group(0)
        
        # Détermination de la catégorie et de la priorité
        informations['categorie'] = pc.get_category_from_text(texte)
        informations['priorite'] = pc.get_priority_from_text(texte)
        informations['texte_original'] = texte
        informations['date_traitement'] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        return informations
    
    def analyser_reclamation(self, texte):
        """
        Analyse une réclamation et génère une réponse appropriée.
        
        Args:
            texte (str): Texte de la réclamation
            
        Returns:
            dict: Résultat de l'analyse avec réponse suggérée
        """
        # Extraction des informations
        infos = self.extraire_informations(texte)
        
        # Génération d'une réponse en fonction de la catégorie
        reponse = self._generer_reponse(infos)
        
        # Ajout de la réponse aux informations
        infos['reponse_suggeree'] = reponse
        
        # Journalisation de l'analyse
        logger.info(f"Réclamation analysée: {infos['categorie']} (Priorité: {infos['priorite']})")
        
        return infos
    
    def _generer_reponse(self, infos):
        """
        Génère une réponse adaptée à la réclamation.
        
        Args:
            infos (dict): Informations extraites de la réclamation
            
        Returns:
            str: Réponse suggérée
        """
        categorie = infos.get('categorie', 'autre')
        
        reponses = {
            "transaction_echouee": "Nous avons bien pris en compte votre signalement concernant l'échec de votre transaction. Notre équipe technique analyse actuellement le problème et reviendra vers vous dans les plus brefs délais.",
            "frais_contestes": "Nous accusons réception de votre contestation concernant les frais appliqués. Un conseiller va examiner votre dossier et vous contactera sous 48 heures ouvrées.",
            "retard_execution": "Nous vous prions de nous excuser pour le retard dans l'exécution de votre ordre. Nous mettons tout en œuvre pour résoudre cette situation et vous tiendrons informé de l'avancement.",
            "erreur_montant": "Nous avons bien noté votre signalement concernant une erreur de montant. Notre service comptabilité va vérifier la transaction et procéder aux ajustements nécessaires.",
            "probleme_technique": "Nous sommes désolés pour le désagrément causé par ce problème technique. Notre équipe informatique travaille actuellement à sa résolution.",
            "autre": "Nous avons bien reçu votre réclamation et la traitons avec la plus grande attention. Un conseiller vous contactera prochainement pour vous apporter une réponse personnalisée."
        }
        
        # Personnalisation de la réponse avec les informations disponibles
        reponse = reponses.get(categorie, reponses["autre"])
        
        # Ajout d'informations spécifiques si disponibles
        if 'transaction_id' in infos:
            reponse += f" Référence de votre transaction: {infos['transaction_id']}."
            
        if 'montant' in infos:
            reponse += f" Montant concerné: {infos['montant']}."
            
        # Ajout d'une formule de politesse
        reponse += " Nous vous remercions pour votre patience et restons à votre disposition pour tout complément d'information."
        
        return reponse
    
    def traiter_fichier_reclamations(self, fichier_entree, fichier_sortie=None):
        """
        Traite un fichier contenant plusieurs réclamations.
        
        Args:
            fichier_entree (str): Chemin vers le fichier d'entrée (JSON ou TXT)
            fichier_sortie (str, optional): Chemin vers le fichier de sortie
            
        Returns:
            list: Résultats du traitement
        """
        resultats = []
        
        try:
            # Détermination du format du fichier
            ext = os.path.splitext(fichier_entree)[1].lower()
            
            if ext == '.json':
                # Traitement d'un fichier JSON
                with open(fichier_entree, 'r', encoding='utf-8') as f:
                    reclamations = json.load(f)
                
                if isinstance(reclamations, list):
                    for reclamation in reclamations:
                        if isinstance(reclamation, str):
                            resultats.append(self.analyser_reclamation(reclamation))
                        elif isinstance(reclamation, dict) and 'texte' in reclamation:
                            resultats.append(self.analyser_reclamation(reclamation['texte']))
                
                elif isinstance(reclamations, dict) and 'reclamations' in reclamations:
                    for reclamation in reclamations['reclamations']:
                        if isinstance(reclamation, str):
                            resultats.append(self.analyser_reclamation(reclamation))
                        elif isinstance(reclamation, dict) and 'texte' in reclamation:
                            resultats.append(self.analyser_reclamation(reclamation['texte']))
            
            elif ext == '.txt':
                # Traitement d'un fichier texte (une réclamation par ligne)
                with open(fichier_entree, 'r', encoding='utf-8') as f:
                    for ligne in f:
                        ligne = ligne.strip()
                        if ligne:
                            resultats.append(self.analyser_reclamation(ligne))
            
            else:
                logger.error(f"Format de fichier non pris en charge: {ext}")
                return []
            
            # Sauvegarde des résultats si un fichier de sortie est spécifié
            if fichier_sortie:
                with open(fichier_sortie, 'w', encoding='utf-8') as f:
                    json.dump(resultats, f, ensure_ascii=False, indent=4)
                logger.info(f"Résultats sauvegardés dans {fichier_sortie}")
            
            return resultats
            
        except Exception as e:
            logger.error(f"Erreur lors du traitement du fichier: {str(e)}")
            return []
    
    def sauvegarder_reclamation(self, infos, fichier_sortie):
        """
        Sauvegarde les informations d'une réclamation dans un fichier.
        
        Args:
            infos (dict): Informations de la réclamation
            fichier_sortie (str): Chemin vers le fichier de sortie
            
        Returns:
            bool: True si la sauvegarde a réussi, False sinon
        """
        try:
            # Vérification si le fichier existe déjà
            if os.path.exists(fichier_sortie):
                # Chargement des données existantes
                with open(fichier_sortie, 'r', encoding='utf-8') as f:
                    try:
                        donnees = json.load(f)
                        if not isinstance(donnees, list):
                            donnees = [donnees]
                    except json.JSONDecodeError:
                        donnees = []
            else:
                donnees = []
            
            # Ajout des nouvelles informations
            donnees.append(infos)
            
            # Sauvegarde des données
            with open(fichier_sortie, 'w', encoding='utf-8') as f:
                json.dump(donnees, f, ensure_ascii=False, indent=4)
            
            logger.info(f"Réclamation sauvegardée dans {fichier_sortie}")
            return True
            
        except Exception as e:
            logger.error(f"Erreur lors de la sauvegarde de la réclamation: {str(e)}")
            return False

def main():
    """Fonction principale pour l'exécution en ligne de commande."""
    parser = argparse.ArgumentParser(description="Agent de traitement des réclamations financières")
    
    # Arguments en ligne de commande
    parser.add_argument("--config", help="Chemin vers un fichier de configuration personnalisé")
    parser.add_argument("--input", help="Chemin vers un fichier d'entrée contenant des réclamations")
    parser.add_argument("--output", help="Chemin vers un fichier de sortie pour les résultats")
    parser.add_argument("--text", help="Texte de réclamation à analyser directement")
    
    args = parser.parse_args()
    
    # Initialisation de l'agent
    agent = ReclamationAgent(config_file=args.config)
    
    # Traitement selon les arguments fournis
    if args.input:
        resultats = agent.traiter_fichier_reclamations(args.input, args.output)
        print(f"{len(resultats)} réclamations traitées.")
    
    elif args.text:
        resultat = agent.analyser_reclamation(args.text)
        print(json.dumps(resultat, ensure_ascii=False, indent=4))
        
        if args.output:
            agent.sauvegarder_reclamation(resultat, args.output)
    
    else:
        print("Aucune réclamation à traiter. Utilisez --input ou --text pour spécifier des réclamations.")

if __name__ == "__main__":
    main()