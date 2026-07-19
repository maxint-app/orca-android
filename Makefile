filter-openapi:
	pnpx openapi-format http://localhost:8081/openapi.yaml -o dist/openapi-formatted.yaml --filterFile ./openapi-filters.yaml

generate:
	openapi-generator-cli generate \
        -i dist/openapi-formatted.yaml \
        -g kotlin \
        -o ./orca/src/main/java/com/maxint/orca \
        --library jvm-retrofit2 \
		--global-property models,apis,supportingFiles \
		--additional-properties=useCoroutines=true,omitGradleWrapper=true

.PHONY: filter-openapi