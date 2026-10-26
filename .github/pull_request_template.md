<!--
Title: start with the ticket key, e.g. "OBANYC-4261 Bump XStream to 1.4.21".
Branch: name it after the ticket (OBANYC-4261).
Text inside these arrows is a direction and won't show on the PR. Delete nothing else.
-->

## Ticket
<!-- Replace both XXXX with the ticket number. -->
[OBANYC-XXXX](https://camsys.atlassian.net/browse/OBANYC-XXXX)

## What changed
<!-- 1-3 sentences: what this PR does and why. If it doesn't do everything the ticket asks, say what's left and why. -->

## How it was tested
<!--
Paste the actual output, not "tested locally".
- New or changed code: unit tests added or updated. Name them.
- Dependency bumps: `mvn clean install` on Java 11 plus `mvn dependency:tree -Dincludes=<groupId>` for each affected module, to show the version that actually ships.
- If something can't be tested, say why and how dev will prove it.
-->

## Deploy impact
<!--
Anything beyond a normal release? Config changes, DB changes, other services that need redeploying, a specific deploy order.
How to roll back if it's not the previous release.
Write "None" if it's a normal release.
-->