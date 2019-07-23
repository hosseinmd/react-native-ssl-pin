export namespace ReactNativeSSLPinning {
    interface Cookies {
        [cookieName: string]: string;
    }

    interface Header {
        [headerName: string]: string;
    }

    interface Options {
        body?: string | object,
        credentials?: string,
        headers?: Header;
        method?: 'DELETE' | 'GET' | 'POST' | 'PUT',
        timeoutInterval?: number,
        tag: string,
    }

    interface Response {
        data: string;
        headers: Header;
        status: number;
        statusText: string;
    }

    interface pinningOptions {
        trust: boolean
        certs: string[],
    }
}

export declare function SSLPinning(url: string, options: ReactNativeSSLPinning.pinningOptions): void;
export declare function fetch(url: string, options: ReactNativeSSLPinning.Options): Promise<ReactNativeSSLPinning.Response>;
export declare function getCookies(domain: string): Promise<ReactNativeSSLPinning.Cookies>;
export declare function removeCookieByName(cookieName: string): Promise<void>;
