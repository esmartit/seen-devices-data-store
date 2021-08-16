db.scanApiActivity.aggregate(
    [
        {
            $project: {
                _id: 0,
                clientMac: 1,
                seenTime: 1,
                rssi: 1,
                countryId: 1,
                stateId: 1,
                cityId: 1,
                spotId: 1,
                sensorId: 1,
                brand: 1,
                status: 1,
                zipCode: 1,
                isConnected: 1,
                username: 1,
                gender: 1,
                age: 1,
                membership: 1,
                userZipCode: 1,
                hotspot: 1,
                zone: 1,
                ssid: 1,
                processed: 1,
                dateAtZone: {
                    $toDate: {
                        $dateToString: {
                            format: '%Y-%m-%d',
                            date: '$seenTime',
                            timezone: 'Europe/Madrid'
                        }
                    }
                },
                timePart: {
                    $dateToString: {
                        format: '%H:%M:%S',
                        date: '$seenTime',
                        timezone: 'Europe/Madrid'
                    }
                }
            }
        },
        {
            $match: {
                status: {
                    $ne: 'NO_POSITION'
                },
                dateAtZone: {
                    '$gte': new Date('2021-08-15'),
                    '$lte': new Date('2021-08-15')
                },
                timePart: {
                    '$gte': '00:00:00',
                    '$lte': '23:59:59'
                }
            }
        },
        {
            $group: {
                _id: {
                    dateAtZone: '$dateAtZone',
                    clientMac: '$clientMac',
                    status: '$status',
                    countryId: '$countryId',
                    stateId: '$stateId',
                    cityId: '$cityId',
                    zipCode: '$zipcode',
                    username: '$username',
                    gender: '$gender',
                    age: '$age',
                    membership: '$membership',
                    userZipCode: '$userZipCode',
                    spotId: '$spotId',
                    sensorId: '$sensorId',
                    hotspot: '$hotspot',
                    brand: '$brand',
                    zone: '$zone',
                    ssid: '$ssid',
                },
                minTime: {
                    $min: '$seenTime'
                },
                maxTime: {
                    $max: '$seenTime'
                }
            }
        },
        {
            $project: {
                dateAtZone: '$_id.dateAtZone',
                clientMac: '$_id.clientMac',
                status: '$_id.status',
                countryId: '$_id.countryId',
                stateId: '$_id.stateId',
                cityId: '$_id.cityId',
                zipCode: '$_id.zipcode',
                username: '$_id.username',
                gender: '$_id.gender',
                age: '$_id.age',
                membership: '$_id.membership',
                userZipCode: '$_id.userZipCode',
                spotId: '$_id.spotId',
                sensorId: '$_id.sensorId',
                hotspot: '$_id.hotspot',
                brand: '$_id.brand',
                zone: '$_id.zone',
                ssid: '$_id.ssid',
                _id: 0,
                totalTime : {
                    $cond: [
                        { $eq: [ "$minTime", "$maxTime" ] }, 60000, { $subtract: [ '$maxTime', '$minTime' ]}
                    ]
                },
                minTime: 1,
                maxTime: 1
            }
        },
        {
            $merge: {
                into: 'scanApiActivityDaily',
                on: '_id',
                whenMatched: 'replace',
                whenNotMatched: 'insert'
            }
        }
    ],
    { allowDiskUse: true }
    )
