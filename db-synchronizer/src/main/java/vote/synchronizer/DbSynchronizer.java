package vote.synchronizer;

import move.storage.DbAccessor;
import move.storage.DbConnectorImpl;
import move.storage.DirectVoteStorageImpl;
import okhttp3.HttpUrl;
import vote.fetcher.DirectVoteFetcherImpl;
import vote.fetcher.FileCachedRestClient;
import vote.fetcher.RestClientImpl;

import java.io.File;

public class DbSynchronizer {
    private static final String baseUrl = "https://www.sejm.gov.pl/sejm8.nsf/";
    public static void main(String[] args) {
        Synchronizer synchronizer = new Synchronizer(
            new DirectVoteFetcherImpl(
                HttpUrl.Companion.get(baseUrl),
                new FileCachedRestClient(RestClientImpl.INSTANCE,new File("H:/who-votes-like-you"))
            ),
            new DirectVoteStorageImpl(new DbAccessor(new DbConnectorImpl()))
        );
        synchronizer.initialize();
    }
}
