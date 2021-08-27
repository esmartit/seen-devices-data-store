# seen-devices-data-store

testing 4.0.3
new release



mongorestore --username root --password password --gzip --archive=/bitnami/mongodb/ibacapital-2019.gzip






kubectl cp migration/ibacapital-2019.gzip data-store-mongodb-0:/bitnami/mongodb -c mongodb


mongorestore --username root --password password --gzip --archive=/bitnami/mongodb/ibacapital-2019.gzip --db smartpoke --collection sensorSetting --drop



mongorestore --username spring --password spring --gzip --archive=/bitnami/mongodb/ibacapital-2019.gzip --db smartpoke --collection sensorSetting --drop




mongorestore --username spring --password spring --gzip --archive=/bitnami/mongodb/hourlyScanApiActivity.gzip --db smartpoke --collection hourlyScanApiActivity





mongodump --collection=hourlyScanApiActivity --db=smartpoke --gzip --archive=migration/ibacapital/2019/hourlyScanApiActivity.gzip



mongorestore --username spring --password spring --gzip --archive=/bitnami/mongodb/dailyScanApiActivity.gzip --db smartpoke --collection dailyScanApiActivity


helm delploy example

helm upgrade --install data-store -f seen-devices-data-store/values-demo.yaml esmartit-data-store/seen-devices-data-store --version=3.10.9