package vote.synchronizer;

import move.storage.DbAccessor;
import move.storage.DbConnectorImpl;
import move.storage.DirectVoteStorageImpl;
import okhttp3.OkHttpClient;
import vote.fetcher.DirectVoteFetcherImpl;

public class DbSynchronizer {
    private static final String baseUrl = "https://www.sejm.gov.pl/sejm8.nsf/";
    public static void main(String[] args) {
        Synchronizer synchronizer = new Synchronizer(
            new DirectVoteFetcherImpl(new OkHttpClient(), baseUrl),
            new DirectVoteStorageImpl(new DbAccessor(new DbConnectorImpl()))
        );
        synchronizer.initialize();
    }
}
