CREATE TABLE t_audioitem ( 
  id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), 
  uuid VARCHAR(255) );

CREATE TABLE t_locale ( 
  id INT, 
  language VARCHAR(255), 
  country VARCHAR(100), 
  description VARCHAR(2048) );

CREATE TABLE t_manifest ( 
  id INT );

CREATE TABLE t_audioitem_has_category ( 
  audioitem INT, 
  category INT );

CREATE TABLE t_category ( 
  id INT, 
  title VARCHAR(255), 
  description VARCHAR(2048), 
  parent INT );

CREATE TABLE t_referenced_file ( 
  id INT, 
  location VARCHAR(2048), 
  manifest INT );

CREATE TABLE t_localized_audioitem ( 
  id INT, 
  uuid VARCHAR(255), 
  language INT, 
  location VARCHAR(2048), 
  audioitem INT, 
  manifest INT );

CREATE TABLE t_metadata ( 
  id INT, 
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
  lb_copy_count INT, 
  lb_play_count INT, 
  lb_rating SMALLINT );
