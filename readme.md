# Paidy Take-Home Coding Exercise

## Introduction

This solution take into consideration the following requirements

1. The service returns an exchange rate when provided with 2 supported currencies
2. The rate should not be older than 5 minutes
3. The service should support at least 10,000 successful requests per day with 1 API token
4. The One-Frame service supports a maximum of 1000 requests per day for any given authentication token
5. One-Frame service token: Header required for authentication. 10dc303535874aeccc86a8251e6992f5 is the only accepted value in the current implementation.

My solution is to make one call and save every single RatePair and their price to a dictionary. If multiple calls are made within 90 seconds, the cached dictionary value is returned. If it's been 90 seconds since last call to One-Frame service, we will make a call to update the dictionary and reset the 90 second timer.

The reason for choosing 90 seconds as the time between calls to One-Frame service is that since there are 86400 seconds in a day, if calls to One-Frame service can happen at max once per 90 second, there can be a max of 960 calls per day which is less than the 1000 call limit we have per day given our API key.

The reason for moving docker to 8081 is to not create conflict in with running this app on 8080, while also avoiding CORS problems

## Dependencies

- Scala/SBT
- JDK 11+ (class file 55.0)

## Setup

```bash
# Starts up OneFrame API docker
docker run -p 8081:8080 --name one-frame -d --rm paidyinc/one-frame

# Run application
sbt run
```

```bash
# Stop OneFrame API docker
docker kill one-frame
```

## Testing the app

Try for example hitting from browser or an app like Postman/Insomnia
```bash
http://localhost:8080/rates?from=USD&to=EUR
```

## Assumptions Made
Since there are only 9 currencies supported right now, the number of entries in the dictionary will be 9x8 = 72. This is a reasonable amount of information to save in the dictionary and fetch from just 1 call without worrying about pagination. If the number of supported currency were to increase to for example 100, the amount of all possible currency pair would be 100x99 = 9900, in which case we will have to consider preformance issues. One solution could be only get one exchange rate for a pair of fiat such as CAD/USD, and calculate price(USD/CAD)=price(CAD/USD)^(-1). This will reduce the number of results needed from OneFrame by half.

Another assumption is that communication between this app and OneFrame is steady. Otherwise we will need to think about retry policies.

## Future improvements
Different error message and http status other than 200
Logging of errors and when resources hit critical threshhold

## Other notes
This is my first Scala app and I feel like I have relied on knowledge from other language such as Typescript and Python to arrive at the decision to use dictionary and sturcture core function in one file. I would like to learn more about Scala coventions and standards and think in Scala as I gain more experience using the language.