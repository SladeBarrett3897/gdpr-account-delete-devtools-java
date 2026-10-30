package devtools;

import java.util.ArrayList;
import java.util.List;

public final class AccountDeletionServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> events = new ArrayList<>();
        AccountDeletionService.Operations fake = new AccountDeletionService.Operations() {
            public List<String> listSessions(String userId) { events.add("list sessions"); return List.of("s-1", "s-2"); }
            public void revokeSession(String id) { events.add("revoke " + id); }
            public void revokeKey(String id) { events.add("revoke " + id); }
        };
        AccountDeletionService.DeletionReceipt receipt = new AccountDeletionService(fake)
                .delete(new AccountDeletionService.DeletionRequest("user-123", "key-9", "r-1"));
        List<String> expected = List.of("list sessions", "revoke s-1", "revoke s-2", "revoke key-9");
        if (!events.equals(expected)) throw new AssertionError(events);
        if (!receipt.sessions().equals(List.of("s-1", "s-2"))) throw new AssertionError(receipt);
        if (!receipt.event().equals("account.deletion.completed")) throw new AssertionError(receipt);
        System.out.println("account deletion decision passed");
    }
}
