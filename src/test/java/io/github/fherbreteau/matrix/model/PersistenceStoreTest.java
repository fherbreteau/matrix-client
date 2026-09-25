package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenceStoreTest {

  @TempDir Path tempDir;

  @Test
  void fileSessionStorePersistsAndClearsSession() throws Exception {
    Path path = tempDir.resolve("session.json");
    var firstStore = new FileSessionStore(path);
    Session session =
        Session.from(
            JsonParser.parse(
                "{\"user_id\":\"@a:b\",\"access_token\":\"access\",\"refresh_token\":\"refresh\",\"device_id\":\"D1\",\"home_server\":\"b\",\"expires_in_ms\":42}"));
    firstStore.save(session);

    if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
      var permissions = Files.getPosixFilePermissions(path);
      assertThat(permissions)
          .containsExactlyInAnyOrder(
              PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }

    Session restored = new FileSessionStore(path).current().orElseThrow();
    assertThat(restored)
        .extracting(
            Session::userId,
            Session::accessToken,
            Session::refreshToken,
            Session::expiresInMs,
            Session::deviceId,
            Session::homeserver)
        .containsExactly("@a:b", "access", "refresh", 42L, "D1", "b");
    assertThat(Files.readString(path)).doesNotContain("well_known");

    firstStore.clear();
    assertThat(new FileSessionStore(path).current()).isEmpty();
  }

  @Test
  void fileSessionStoreHandlesRefreshableAndMinimalSessionFields() {
    Path path = tempDir.resolve("session-min.json");
    var store = new FileSessionStore(path);
    store.save(Session.from(JsonParser.parse("{\"user_id\":\"@b:x\",\"access_token\":\"t\"}")));
    Session restored = store.current().orElseThrow();
    assertThat(restored)
        .extracting(Session::userId, Session::deviceId)
        .containsExactly("@b:x", null);
  }

  @Test
  void fileSyncTokenStorePersistsOpaqueTokenAndClears() {
    Path path = tempDir.resolve("sync.json");
    var firstStore = new FileSyncTokenStore(path);
    firstStore.save("opaque/token?x=1");
    assertThat(new FileSyncTokenStore(path).current()).contains("opaque/token?x=1");
    firstStore.clear();
    assertThat(new FileSyncTokenStore(path).current()).isEmpty();
  }

  @Test
  void fileTransactionIdStoreReusesIdsAcrossInstancesUntilCompleted() {
    Path path = tempDir.resolve("transactions.json");
    var firstStore = new FileTransactionIdStore(path);
    firstStore.complete("missing");
    String transactionId = firstStore.getOrCreate("send:operation:1");
    assertThat(transactionId).isNotBlank();
    assertThat(new FileTransactionIdStore(path).getOrCreate("send:operation:1"))
        .isEqualTo(transactionId);
    assertThat(firstStore.find("send:operation:1")).contains(transactionId);
    firstStore.complete("send:operation:1");
    assertThat(new FileTransactionIdStore(path).find("send:operation:1")).isEmpty();
    firstStore.complete("send:operation:1");
    firstStore.getOrCreate("send:operation:2");
    firstStore.getOrCreate("send:operation:3");
    firstStore.complete("send:operation:2");
    assertThat(firstStore.find("send:operation:2")).isEmpty();
    assertThat(firstStore.find("send:operation:3")).isPresent();
    assertThat(firstStore.getOrCreate("send:operation:1")).isNotEqualTo(transactionId);
  }

  @Test
  void inMemoryTransactionIdStoreReusesIdsUntilCompleted() {
    TransactionIdStore store = TransactionIdStore.inMemory();
    String id = store.getOrCreate("operation");
    assertThat(store.getOrCreate("operation")).isEqualTo(id);
    store.complete("operation");
    assertThat(store.find("operation")).isEmpty();
  }

  @Test
  void inMemoryMetadataStoreSupportsGetPutAndRemove() {
    MediaMetadataStore store = MediaMetadataStore.inMemory();
    var metadata = JsonParser.parse("{\"cache\":true}");
    store.put("mxc://example.org/id", metadata);
    assertThat(store.get("mxc://example.org/id")).isPresent();
    store.remove("mxc://example.org/id");
    assertThat(store.get("mxc://example.org/id")).isEmpty();
  }

  @Test
  void fileMetadataStorePersistsAndRemovesJsonMetadata() {
    Path path = tempDir.resolve("media-metadata.json");
    var firstStore = new FileMediaMetadataStore(path);
    var metadata = JsonParser.parse("{\"width\":640,\"mime\":\"image/png\"}");
    firstStore.put("mxc://example.org/id", metadata);
    assertThat(new FileMediaMetadataStore(path).get("mxc://example.org/id").orElseThrow().toJson())
        .isEqualTo(metadata.toJson());
    firstStore.put("mxc://example.org/other", JsonParser.parse("{\"width\":320}"));
    firstStore.remove("mxc://example.org/id");
    assertThat(new FileMediaMetadataStore(path).get("mxc://example.org/id")).isEmpty();
    assertThat(new FileMediaMetadataStore(path).get("mxc://example.org/other")).isPresent();
  }

  @Test
  void concurrentTransactionIdRequestsFromSeparateStoresShareOneId() throws Exception {
    Path path = tempDir.resolve("cross-store-transactions.json");
    var stores =
        List.of(
            new FileTransactionIdStore(path),
            new FileTransactionIdStore(path),
            new FileTransactionIdStore(path),
            new FileTransactionIdStore(path));
    try (var executor = Executors.newFixedThreadPool(stores.size())) {
      var start = new CountDownLatch(1);
      var futures = new ArrayList<java.util.concurrent.Future<String>>();
      for (var store : stores) {
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  return store.getOrCreate("one-operation");
                }));
      }
      start.countDown();
      var ids = new java.util.HashSet<String>();
      for (var future : futures) {
        ids.add(future.get(5, TimeUnit.SECONDS));
      }
      assertThat(ids).hasSize(1);
    }
  }

  @Test
  void concurrentTransactionIdRequestsShareOneId() throws Exception {
    var store = new FileTransactionIdStore(tempDir.resolve("concurrent-transactions.json"));
    try (var executor = Executors.newFixedThreadPool(8)) {
      var start = new CountDownLatch(1);
      var futures = new ArrayList<java.util.concurrent.Future<String>>();
      for (int i = 0; i < 32; i++) {
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  return store.getOrCreate("one-operation");
                }));
      }
      start.countDown();
      var ids = new java.util.HashSet<String>();
      for (var future : futures) {
        ids.add(future.get(5, TimeUnit.SECONDS));
      }
      assertThat(ids).hasSize(1);
    }
  }

  @Test
  void concurrentSessionSavesRemainReadable() throws Exception {
    var store = new FileSessionStore(tempDir.resolve("concurrent-session.json"));
    Session session =
        Session.from(JsonParser.parse("{\"user_id\":\"@a:b\",\"access_token\":\"t\"}"));
    try (var executor = Executors.newFixedThreadPool(4)) {
      var futures = new ArrayList<java.util.concurrent.Future<?>>();
      for (int i = 0; i < 12; i++) {
        futures.add(executor.submit(() -> store.save(session)));
      }
      for (var future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }
    }
    assertThat(store.current())
        .get()
        .extracting(Session::userId, Session::accessToken)
        .containsExactly("@a:b", "t");
  }
}
