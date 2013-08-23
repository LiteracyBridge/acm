namespace java org.literacybridge.acm.thrift

struct ThriftPlaylistMapping {
  1: required string audioLabel
  2: required list<string> audioItemUIDs

  // a locked label doesn't allow a talking book user to add/remove recordings
  3: optional bool locked=0
}

struct ThriftDeviceProfile {
  1: required string name
  2: required list<ThriftPlaylistMapping> playlists

  // whether or not to erase the "other" list when the image is applied to a talking book device
  3: optional bool eraseOtherList=0

  4: optional string language

  // points to a file that defines mappings of user input to system audio prompts
  5: optional string controlMapping
}

struct ThriftDeviceImage {
  1: required string name
  2: required list<ThriftDeviceProfile> profiles
}