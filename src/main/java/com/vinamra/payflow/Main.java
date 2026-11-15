package com.vinamra.payflow;

import com.vinamra.payflow.api.ApiServer;
import com.vinamra.payflow.repository.InMemoryAccountRepository;
import com.vinamra.payflow.repository.InMemoryTransactionRepository;
import com.vinamra.payflow.repository.InMemoryUserRepository;
import com.vinamra.payflow.repository.UserRepository;
import com.vinamra.payflow.service.AdminService;
import com.vinamra.payflow.service.AuthService;
import com.vinamra.payflow.service.WalletService;

import java.security.SecureRandom;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        UserRepository users = new InMemoryUserRepository();
        WalletService wallet = new WalletService(
                new InMemoryAccountRepository(), new InMemoryTransactionRepository());
        AuthService auth = new AuthService(users);
        AdminService admin = new AdminService(wallet, users);

        // The bank owner is created here, at start-up. There is no sign-up route for it.
        String ownerUser = System.getenv().getOrDefault("PAYFLOW_ADMIN_USER", "owner");
        String ownerPass = System.getenv("PAYFLOW_ADMIN_PASSWORD");
        boolean generated = ownerPass == null || ownerPass.length() < 8;
        if (generated) {
            ownerPass = randomPassword(12);
        }
        auth.createAdmin(ownerUser, ownerPass, "Bank Owner");

        ApiServer server = new ApiServer(wallet, auth, admin, port);
        server.start();
        System.out.println("PayFlow is running on http://localhost:" + server.getPort());
        System.out.println();
        System.out.println("=== Bank owner login (sees every account and transaction) ===");
        System.out.println("Username: " + ownerUser);
        System.out.println("Password: " + ownerPass + (generated
                ? "   (generated for this run; set PAYFLOW_ADMIN_PASSWORD to choose your own)" : ""));
        System.out.println();
        System.out.println("Customers register on the web page. Press Ctrl+C to stop.");
    }

    private static String randomPassword(int length) {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }
}
