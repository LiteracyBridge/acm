namespace java org.literacybridge.acm.thrift

enum ThriftAudioItemStatus {
  CURRENT,
  NO_LONGER_USED
}

struct ThriftCategory {
  1: required i32 uid
}

struct ThriftPlaylist {
  1: required i32 uid
}

struct ThriftLanguage {
  1: required string languageCode
  2: optional string countryCode
}

struct ThriftAudioItem {
  1: required i32 uid
  2: required ThriftAudioItemStatus status = ThriftAudioItemStatus.CURRENT
  3: required i32 revision
  4: optional string title
  5: optional string duration
  6: optional list<ThriftCategory> categories
  7: optional list<ThriftPlaylist> playlists
  8: optional ThriftLanguage language
  9: optional string messageFormat
 10: optional string targetAudience
 11: optional i64 dateRecorded
 12: optional list<string> keywords
 13: optional string timing
 14: optional string source
 15: optional string primarySpeaker
 16: optional string goal
 17: optional string deviceId
 18: optional string messageId
 19: optional list<string> relatedMessageIds
 20: optional string englishTranscription
 21: optional string notes
 22: optional string beneficiary
}