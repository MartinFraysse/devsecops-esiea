# DevSecOps : injection SQL dans `/account/balance`

## La vulnérabilité

Le paramètre `iban` est concaténé tel quel dans la requête SQL (CWE-89, OWASP A03:2021).
Voir `BalanceController_vuln.java`.

Exemple d'attaque : `GET /account/balance?iban=' OR '1'='1` donne
`SELECT balance FROM accounts WHERE iban = '' OR '1'='1'`, qui renvoie le solde du premier
compte de la table. Avec `' UNION SELECT password FROM users --`, on lit d'autres tables.

Défauts secondaires :
- `Connection`, `Statement` et `ResultSet` jamais fermés : fuite de connexions, puis pool épuisé.
- La `SQLException` remonte au client et peut révéler la structure de la base.

## La correction

Voir `BalanceController_fixed.java`. La requête préparée (`PreparedStatement` + `?`) suffit à
bloquer l'injection : l'IBAN est envoyé comme donnée, jamais interprété comme du SQL.

| Mesure | Risque traité | Ce qui change |
| --- | --- | --- |
| `PreparedStatement` + `?` | Injection SQL | `' OR '1'='1` est cherché comme un IBAN littéral et ne trouve rien |
| Validation par regex | Entrées malformées | Refus en 400 avant d'atteindre la base |
| try-with-resources | Fuite de connexions | Ressources fermées même en cas d'exception |
| `catch` + message générique | Fuite d'informations | Erreur loguée côté serveur, client voit « Erreur interne » |
| `BigDecimal` | Erreurs d'arrondi | Montant exact, sans `double` |

En situation réelle, ajouter le contrôle d'appartenance du compte
(`WHERE iban = ? AND owner_id = ?`), sinon n'importe qui lit n'importe quel solde (IDOR, OWASP A01:2021).

## Côté DevSecOps

- Compte SQL applicatif en lecture seule sur `accounts` (moindre privilège).
- SAST dans la CI (SonarQube, Semgrep, CodeQL) : détecte le SQL construit depuis une entrée utilisateur.
- DAST (OWASP ZAP) ou `sqlmap` sur l'environnement de recette.
