// Version VULNERABLE (injection SQL) — ne pas utiliser
@GetMapping("/account/balance")
public String getBalance(@RequestParam("iban") String iban) throws SQLException {
    Connection conn = dataSource.getConnection();
    // VULNERABILITE ICI :
    String sql = "SELECT balance FROM accounts WHERE iban = '" + iban + "'";
    ResultSet rs = conn.createStatement().executeQuery(sql);
    if (rs.next()) { return rs.getString("balance"); }
    return "0.0";
}
