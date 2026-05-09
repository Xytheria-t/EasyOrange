export interface RequestConfig extends RequestInit {
    headers: Record<string, string>;
}

export type RequestInterceptor = (config: RequestConfig) => RequestConfig | Promise<RequestConfig>;
export type ResponseInterceptor = (response: Response) => Response | Promise<Response>;

const requestInterceptors: RequestInterceptor[] = [];
const responseInterceptors: ResponseInterceptor[] = [];

export const addRequestInterceptor = (interceptor: RequestInterceptor): (() => void) => {
    requestInterceptors.push(interceptor);
    return () => {
        const index = requestInterceptors.indexOf(interceptor);
        if (index > -1) {requestInterceptors.splice(index, 1);}
    };
};

export const addResponseInterceptor = (interceptor: ResponseInterceptor): (() => void) => {
    responseInterceptors.push(interceptor);
    return () => {
        const index = responseInterceptors.indexOf(interceptor);
        if (index > -1) {responseInterceptors.splice(index, 1);}
    };
};

export const applyRequestInterceptors = async (config: RequestConfig): Promise<RequestConfig> => {
    let result = config;
    for (const interceptor of requestInterceptors) {
        result = await interceptor(result);
    }
    return result;
};

export const applyResponseInterceptors = async (response: Response): Promise<Response> => {
    let result = response;
    for (const interceptor of responseInterceptors) {
        result = await interceptor(result);
    }
    return result;
};
