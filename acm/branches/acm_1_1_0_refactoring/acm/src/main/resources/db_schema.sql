CREATE TABLE t_sequence ( 
  seq_name VARCHAR(50), 
  seq_count INT );

CREATE TABLE t_localized_string ( 
  id INT PRIMARY KEY, 
  string INT,
  translation_string VARCHAR(2048), 
  locale INT );

CREATE TABLE t_audioitem_has_category ( 
  audioitem INT, 
  category INT,
  PRIMARY KEY(audioitem,category));

CREATE TABLE t_audioitem_has_tag ( 
  audioitem INT, 
  tag INT,
  ordering INT,
  PRIMARY KEY(audioitem,tag) );

CREATE TABLE t_referenced_file ( 
  id INT PRIMARY KEY, 
  location VARCHAR(2048), 
  manifest INT );

CREATE TABLE t_manifest ( 
  id INT PRIMARY KEY);

CREATE TABLE t_category ( 
  id INT PRIMARY KEY, 
  uuid VARCHAR(255), 
  revision INT,
  lang_title INT, 
  lang_desc INT, 
  parent INT );

CREATE TABLE t_tag ( 
  id INT PRIMARY KEY, 
  uuid VARCHAR(255), 
  name VARCHAR(255) );

CREATE TABLE t_metadata ( 
  id INT PRIMARY KEY, 
  dc_contributor VARCHAR(255), 
  dc_coverage VARCHAR(255), 
  dc_creator VARCHAR(100), 
  dc_date DATE, 
  dc_description VARCHAR(2048), 
  dc_format VARCHAR(50), 
  dc_identifier VARCHAR(50), 
  dc_language INT, 
  dc_publisher VARCHAR(100), 
  dc_relation VARCHAR(255), 
  dc_rights VARCHAR(255), 
  dc_source VARCHAR(255), 
  dc_subject VARCHAR(255), 
  dc_title VARCHAR(255), 
  dc_type VARCHAR(50), 
  dtb_revision VARCHAR(50), 
  dtb_revision_date DATE, 
  dtb_revision_description VARCHAR(2048),
  lb_rating SMALLINT );

CREATE TABLE t_audioitem ( 
  id INT PRIMARY KEY, 
  uuid VARCHAR(255) );

CREATE TABLE t_string ( 
  id INT PRIMARY KEY, 
  context VARCHAR(2048) );

CREATE TABLE t_localized_audioitem ( 
  id INT PRIMARY KEY, 
  uuid VARCHAR(255), 
  language INT, 
  location VARCHAR(2048), 
  audioitem INT, 
  metadata INT, 
  manifest INT );

CREATE TABLE t_locale ( 
  id INT PRIMARY KEY, 
  language VARCHAR(255), 
  country VARCHAR(100), 
  description VARCHAR(2048) );
  
CREATE TABLE t_audioitem_statistic ( 
  id INT PRIMARY KEY, 
  metadata INT,  
  device_id VARCHAR(255), 
  boot_cycle_number INT,
  lb_copy_count INT, 
  lb_open_count INT,
  lb_completion_count INT,
  lb_survey1_count INT,
  lb_apply_count INT,
  lb_useless_count INT);  
  
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_audioitem',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_localized_audioitem',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_category',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_localized_string',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_locale',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_manifest',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_metadata',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_referenced_file',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_string',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_audioitem_statistic',0);
INSERT INTO t_sequence(seq_name,seq_count) VALUES('gen_tag',0);
