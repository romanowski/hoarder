package org.romanowski.hoarder.tests

import org.romanowski.hoarder.actions.CachedCI
import org.romanowski.hoarder.actions.ci.TravisPRValidation

object IntegrationTestSetup extends TravisPRValidation {
  override def shouldPublishCaches(): Boolean = true
  override def shouldUseCache(): Boolean = sys.env.get("HOARDER_CACHE_EXPORTED").isDefined
}

class IntegrationTestFlowBase extends CachedCI.PluginBase(IntegrationTestSetup)