import axios, { Axios } from 'axios';
import { environment } from '../environment.ts';
import { TutorialType } from '../types/tutorial.type.ts';

export class DataService {
    private readonly client?: Axios;

    constructor() {
        this.client = axios.create({
            baseURL: environment.API_BASE_URL,
            headers: {
                'Content-Type': 'application/json'
            }
        });
        console.log(`... created axios http client to base url = ${this.client?.defaults.baseURL} ...`);
    }

    async getTutorials(): Promise<TutorialType[]> {
        return this.getHttpData<TutorialType[]>('/tutorials').catch(error => {
            console.error(error);
            return Promise.resolve([]);
        });
    }

    async getTutorial(id: string): Promise<TutorialType> {
        return this.getHttpData<TutorialType>('/tutorials/' + id).catch(error => {
            console.error(error);
            return Promise.resolve({});
        });
    }

    async getTutorialByTitle(title: string): Promise<TutorialType> {
        return this.getHttpData<TutorialType>('/tutorial/title/' + title).catch(error => {
            console.error(error);
            return Promise.resolve({});
        });
    }

    async postTutorial(title: string, description: string): Promise<void> {
        const http = await this.checkClient();
        return http.post('/add/tutorial', {
            title,
            description
        });
    }

    private async getHttpData<R>(path: string): Promise<R> {
        try {
            return await this.checkClient()
                .then(client => client.get(path))
                .then(response => response.data as R);
        } catch (error) {
            console.error(error);
            return Promise.reject(new Error('An error while getting http response occurred!'));
        }
    }

    private async checkClient(): Promise<Axios> {
        try {
            return await new Promise((resolve, reject) => {
                if (!this.client) {
                    reject(new Error('HTTP client was not initialized!'));
                } else {
                    resolve(this.client);
                }
            });
        } catch (error) {
            console.error(error);
            return Promise.reject(new Error('An error on axios http client occurred!'));
        }
    }
}
