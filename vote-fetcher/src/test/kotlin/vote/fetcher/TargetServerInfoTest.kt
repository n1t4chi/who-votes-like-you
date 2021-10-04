package vote.fetcher

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TargetServerInfoTest{
    @Test
    fun urlBuilder_protocolAndHostnameCase() {
        val info = TargetServerInfo("http", "hostname")
        Assertions.assertEquals( "http://hostname/", info.urlBuilder().build().toString() )
    }

    @Test
    fun urlBuilder_withPortCase() {
        val info = TargetServerInfo("http", "hostname", 60 )
        Assertions.assertEquals( "http://hostname:60/", info.urlBuilder().build().toString() )
    }

    @Test
    fun urlBuilder_withFileCase() {
        val info = TargetServerInfo("http", "hostname", baseFile = "rootDir" )
        Assertions.assertEquals( "http://hostname/rootDir", info.urlBuilder().build().toString() )
    }

    @Test
    fun urlBuilder_allArgs() {
        val info = TargetServerInfo("http", "hostname", 60, "rootDir" )
        Assertions.assertEquals( "http://hostname:60/rootDir", info.urlBuilder().build().toString() )
    }

    @Test
    fun urlBuilder_canAddPath() {
        val info = TargetServerInfo("http", "hostname", 60, "rootDir" )
        Assertions.assertEquals(
            "http://hostname:60/rootDir/pathTo",
            info.urlBuilder()
                .addPathSegment("pathTo")
                .build()
                .toString()
        )
    }

    @Test
    fun urlBuilder_canAddQueryParam() {
        val info = TargetServerInfo("http", "hostname", 60, "rootDir" )
        Assertions.assertEquals(
            "http://hostname:60/rootDir?myArg=text",
            info.urlBuilder()
                .addQueryParameter("myArg", "text" )
                .build()
                .toString()
        )
    }
}