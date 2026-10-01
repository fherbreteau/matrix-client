package io.github.fherbreteau.matrix.transport;

final class UrlRedaction {

  private UrlRedaction() {}

  static String redactQueryAndFragment(String url) {
    int query = url.indexOf('?');
    int fragment = url.indexOf('#');
    int end = url.length();
    if (query >= 0) {
      end = query;
    }
    if (fragment >= 0) {
      end = Math.min(end, fragment);
    }
    return url.substring(0, end);
  }
}
