package devtools;

public final class AccountDeletionCli {
    private AccountDeletionCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: AccountDeletionCli <user_id> <key_id>");
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        InfraiClient client = new InfraiClient(java.net.http.HttpClient.newHttpClient(), key);
        AccountDeletionService service = new AccountDeletionService(new AccountDeletionService.Operations() {
            public java.util.List<String> listSessions(String userId) throws java.io.IOException, InterruptedException { return client.listSessions(userId); }
            public void revokeSession(String sessionId) throws java.io.IOException, InterruptedException { client.revokeSession(sessionId); }
            public void revokeKey(String keyId) throws java.io.IOException, InterruptedException { client.revokeKey(keyId); }
        });
        System.out.println(service.delete(new AccountDeletionService.DeletionRequest(args[0], args[1], "release-" + System.currentTimeMillis())));
    }
}
