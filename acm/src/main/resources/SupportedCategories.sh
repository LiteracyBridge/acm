#!/usr/bin/env zsh
traditionalIFS="$IFS"
#IFS="`printf '\n\t'`"
set -u

# Script to create and initially populate the recipients table.

if [ -z "${psql:-}" ]; then
    if [ -e /Applications/Postgres.app/Contents/Versions/latest/bin/psql ]; then
        psql=/Applications/Postgres.app/Contents/Versions/latest/bin/psql
    elif [ -e /Applications/Postgres.app/Contents/Versions/9.5/bin/psql ]; then
        psql=/Applications/Postgres.app/Contents/Versions/9.5/bin/psql
    elif [ -e /Applications/Postgres.app/Contents/Versions/9.4/bin/psql ]; then
        psql=/Applications/Postgres.app/Contents/Versions/9.4/bin/psql
    elif [ ! -z "$(which psql)" ]; then
        psql=$(which psql)
    else
        echo "Can't find psql!"
        exit 100
    fi
fi
if [ -z "${dbcxn:-}" ]; then
    dbcxn=(--host=lb-device-usage.ccekjtcevhb7.us-west-2.rds.amazonaws.com --port 5432 --username=lb_data_uploader --dbname=dashboard)
    # dbcxn=(--host=localhost --port=5432 --username=lb_data_uploader --dbname=dashboard)

fi

function configure() {
    categoriesfile="SupportedCategories.csv"

    echo $(date)>log.txt

    echo "psql: ${psql}" 
    echo "dbcxn: ${dbcxn}"
}

function main() {
    configure

    set -x
    
    importTable
}



function importTable() {

    # Import into db, create table if necessary
    ${psql} ${dbcxn} -a <<EndOfQuery >>log.txt
    \\timing
    \\set ECHO all
    BEGIN TRANSACTION;    
    create table if not exists supportedcategories (
        categorycode char varying primary key,
        parentcategory char varying references supportedcategories (categorycode),
        isleafnode boolean not null,
        categoryname char varying not null,
        fullname char varying not null
    );
    
    create temp table temp (like supportedcategories);
    \copy temp from '${categoriesfile}' with delimiter ',' csv;

    insert into supportedcategories select * from temp
      on conflict (categorycode) do update
        set parentcategory=EXCLUDED.parentcategory, categoryname=EXCLUDED.categoryname,
            fullname=EXCLUDED.fullname;

    delete from supportedcategories where categorycode not in (select categorycode from temp);

    select * from supportedcategories;

    --abort;
    commit;

EndOfQuery
}

main "$@"

# ta-da

