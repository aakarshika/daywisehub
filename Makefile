.PHONY: help ios-run ios-run-bg ios-device ios-live

help: ## List the commands
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-12s %s\n", $$1, $$2}'

ios-run: ## Build iOS and run it on the phone or simulator picked in Xcode, no Xcode clicks
	@scripts/ios.sh deploy

ios-run-bg: ## Same as ios-run, in the background: returns at once, output goes to build/ios-run.log
	@mkdir -p build
	@nohup scripts/ios.sh deploy > build/ios-run.log 2>&1 &
	@echo "iOS build triggered."

ios-device: ## Open Xcode. Pick your iPhone or a simulator there and press Run
	@scripts/ios.sh device

ios-live: ## Open Xcode, press Run once, then every save runs it again on that target. Ctrl+C to stop
	@scripts/ios.sh live
