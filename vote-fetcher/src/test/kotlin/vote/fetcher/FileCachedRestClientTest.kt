package vote.fetcher

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import org.mockito.Mockito
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.io.path.createTempDirectory

class FileCachedRestClientTest {
    companion object {
        val directory = createTempDirectory("FileCachedRestClientTest" ).toFile()
        val mockClient = Mockito.mock(RestClient::class.java)
        val fileCachedClient = FileCachedRestClient(mockClient, directory)
    
        @JvmStatic
        private fun provideResponseDatas(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(ResponseData(123, false, "fasdfasd\nadsfasd\nrfasdf\nasdf") ),
                Arguments.of(ResponseData(5001, false, "<html>\n\t<div>\n\t\ttext\n\t<div/>\n<html/>") ),
                Arguments.of(ResponseData(4, true, "!@#$%^&*()[]{};'\\:\"|,./<>?") ),
                Arguments.of(ResponseData(401, true, "<html>\n\t<div>\n\t\ttext\n\t<div/>\n<html/>") )
            )
        }
    }
    
    @AfterEach
    fun tearDown() {
        directory.deleteRecursively()
    }
    
    @Test
    internal fun encodeCreatesProperFileNames() {
        Assertions.assertEquals(
            "httpstronasejmuglosykadencja7dzien123glosowanieGlosowanie123.cache",
            fileCachedClient.encode("http://strona.sejmu/glosy?kadencja=7&dzien=123&glosowanie=Glosowanie 123".toHttpUrl())
        )
    }
    
    @ParameterizedTest
    @MethodSource("provideResponseDatas")
    internal fun canSaveToFile_andMatchRegex(data: ResponseData) {
        //setup
        val file = directory.resolve("file.cache")
        //execute
        fileCachedClient.writeToFile(file, data)
        //verify
        val content = Files.readString(file.toPath())
        Assertions.assertEquals(
            "code:${data.code}\nisSuccess:${data.isSuccess}\nbody:\n${data.body}",
            content
        )
        Assertions.assertTrue(FileCachedRestClient.fileFormat.matches(content))
    }
    
    @Test
    internal fun canReadFile() {
        //setup
        val data = ResponseData(401, true, "<html>\n\t<div>\n\t\ttext\n\t<div/>\n<html/>")
        val content = "code:${data.code}\nisSuccess:${data.isSuccess}\nbody:\n${data.body}"
        directory.mkdirs()
        val file = directory.resolve("file.cache")
        Files.writeString(file.toPath(),content)
        
        //execute
        val parsedData = fileCachedClient.readFile(file)
        
        //verify
        Assertions.assertEquals(data, parsedData)
    }
    
    @Test
    internal fun onGet_urlNotAccessedBefore_callsMockClient_savesResultToFile() {
        //setup
        val data = ResponseData(401, true, "<html>\n\t<div>\n\t\ttext\n\t<div/>\n<html/>")
        val url = "http://localhost/get".toHttpUrl()
        val file = directory.resolve("httplocalhostget.cache")
        Mockito.doReturn(data).`when`(mockClient).get(url)
    
        //execute
        val parsedData = fileCachedClient.get(url)
        
        //verify
        Assertions.assertEquals(data, parsedData)
        Mockito.verify(mockClient).get(url)
        Assertions.assertEquals(
            "code:${data.code}\nisSuccess:${data.isSuccess}\nbody:\n${data.body}",
            Files.readString(file.toPath())
        )
    }
    
    @Test
    internal fun onGet_urlAccessedBefore_loadsFromFile_doesNotCallMockClient() {
        //setup
        val data = ResponseData(401, true, "<html>\n\t<div>\n\t\ttext\n\t<div/>\n<html/>")
        val content = "code:${data.code}\nisSuccess:${data.isSuccess}\nbody:\n${data.body}"
        directory.mkdirs()
        val url = "http://localhost/get".toHttpUrl()
        val file = directory.resolve("httplocalhostget.cache")
        Files.writeString(file.toPath(),content)
        
        //execute
        val parsedData = fileCachedClient.get(url)
        
        //verify
        Assertions.assertEquals(data, parsedData)
        Mockito.verify(mockClient,Mockito.never()).get(url)
    }
}