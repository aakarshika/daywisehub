.PHONY: help ios-device ios-live

help: ## List the commands
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-12s %s\n", $$1, $$2}'

ios-device: ## Open Xcode. Pick your iPhone or a simulator there and press Run
	@scripts/ios.sh device

ios-live: ## Open Xcode, press Run once, then every save runs it again on that target. Ctrl+C to stop
	@scripts/ios.sh live
