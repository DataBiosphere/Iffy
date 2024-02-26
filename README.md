## Iffy

This is a proof-of-concept implementation of a feature flag evaluation service for Terra,
integrating components from the OpenFeature project?


### Running Tests

    gradle build

### Starting Iffy Locally

First, [download a binary distribution of Flagd](https://flagd.dev/installation/). Then, run it
locally with the sample flag spec:

    # This command assumes you are in the root directory of the iffy project

    flagd start -f "file:///$( pwd )/flagd.example.json"

Then start Iffy:

    export IFFY_ADDRESS=http://localhost:8080   # Necessary for hacky hello world poc controller
    gradle bootRun
