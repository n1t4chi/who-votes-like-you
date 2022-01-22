package vote.synchronizer;

import move.storage.DirectVoteStorageImpl;
import move.storage.access.*;
import okhttp3.HttpUrl;
import vote.fetcher.DirectVoteFetcherImpl;
import vote.fetcher.restclient.*;

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
