module io.github.fherbreteau.matrix {
  exports io.github.fherbreteau.matrix.json;
  exports io.github.fherbreteau.matrix.transport;
  exports io.github.fherbreteau.matrix.store;
  exports io.github.fherbreteau.matrix.store.file;
  exports io.github.fherbreteau.matrix.store.memory;
  exports io.github.fherbreteau.matrix.retry;
  exports io.github.fherbreteau.matrix.error;
  exports io.github.fherbreteau.matrix.model;
  exports io.github.fherbreteau.matrix.model.events;
  exports io.github.fherbreteau.matrix.endpoint;

  requires transitive java.net.http;
}
