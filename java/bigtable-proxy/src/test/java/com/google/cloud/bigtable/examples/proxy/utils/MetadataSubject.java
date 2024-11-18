package com.google.cloud.bigtable.examples.proxy.utils;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import io.grpc.Metadata;
import java.util.ArrayList;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class MetadataSubject extends Subject {
  private final Metadata metadata;

  public MetadataSubject(FailureMetadata metadata, @Nullable Metadata actual) {
    super(metadata, actual);
    this.metadata = actual;
  }

  public static Factory<MetadataSubject, Metadata> metadata() {
    return MetadataSubject::new;
  }

  public static MetadataSubject assertThat(Metadata metadata) {
    return assertAbout(metadata()).that(metadata);
  }

  public void hasKey(String key) {
    hasKey(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
  }

  public void hasKey(Metadata.Key<?> key) {
    check("keys()").that(metadata.keys()).contains(key);
  }

  public void hasValue(String key, String value) {
    hasValue(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);
  }

  public <T> void hasValue(Metadata.Key<T> key, T value) {
    Iterable<T> actualValues = Optional.ofNullable(metadata.getAll(key)).orElse(new ArrayList<>());
    check("get(" + key + ")").that(actualValues).containsExactly(value);
  }

  public void containsValue(String key, String value) {
    check("get(" + key + ")")
        .that(metadata.getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)))
        .contains(value);
  }

  public <T> void containsValue(Metadata.Key<T> key, T value) {
    check("get(" + key + ")").that(metadata.getAll(key)).contains(value);
  }
}
