// Version CORRIGEE
private static final Pattern IBAN_PATTERN =
        Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");

@GetMapping("/account/balance")
public ResponseEntity<String> getBalance(@RequestParam("iban") String iban) {
    // 1. Validation de l'entree (liste blanche)
    String cleanIban = iban.replace(" ", "").toUpperCase();
    if (!IBAN_PATTERN.matcher(cleanIban).matches()) {
        return ResponseEntity.badRequest().body("IBAN invalide");
    }

    // 2. Requete parametree + fermeture automatique des ressources
    String sql = "SELECT balance FROM accounts WHERE iban = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, cleanIban);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return ResponseEntity.ok(rs.getBigDecimal("balance").toPlainString());
            }
            return ResponseEntity.notFound().build();
        }
    } catch (SQLException e) {
        // 3. Pas de detail technique renvoye au client
        log.error("Erreur lors de la lecture du solde", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur interne");
    }
}

// Variante avec Spring JdbcTemplate (parametrage + fermeture geres par Spring) :
// BigDecimal balance = jdbcTemplate.queryForObject(
//         "SELECT balance FROM accounts WHERE iban = ?", BigDecimal.class, cleanIban);
