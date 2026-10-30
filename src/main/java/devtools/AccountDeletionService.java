package devtools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AccountDeletionService {
    private final Operations operations;

    public AccountDeletionService(Operations operations) { this.operations = operations; }

    public DeletionReceipt delete(DeletionRequest request) throws IOException, InterruptedException {
        List<String> sessions = operations.listSessions(request.userId());
        List<String> revoked = new ArrayList<>();
        for (String session : sessions) { operations.revokeSession(session); revoked.add(session); }
        operations.revokeKey(request.keyId());
        return new DeletionReceipt(request.userId(), revoked, request.keyId(),
                "account.deletion.completed", "release=" + request.releaseId());
    }

    public record DeletionRequest(String userId, String keyId, String releaseId) {}
    public record DeletionReceipt(String userId, List<String> sessions, String keyId, String event, String diagnostic) {}

    public interface Operations {
        List<String> listSessions(String userId) throws IOException, InterruptedException;
        void revokeSession(String sessionId) throws IOException, InterruptedException;
        void revokeKey(String keyId) throws IOException, InterruptedException;
    }
}
