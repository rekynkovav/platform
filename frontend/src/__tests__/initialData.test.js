import {
    initialBroadcasts,
    initialBroadcastsGuests,
    initialExercises,
    initialRecommendations,
    initialHardening,
    initialAbout,
    initialNewbies,
    initialHeartVoice,
    initialReflection,
    initialInterviews,
    initialParticipants,
    initialMotivation,
    initialReviews,
    initialMusic,
    initialMovies,
    initialLiterature,
    initialAboutTimoshenko,
    initialChallenge,
} from '../components/Dashboard/initialData';

describe('initialData', () => {
    it('exports all 18 data arrays', () => {
        expect(initialBroadcasts).toBeDefined();
        expect(initialBroadcastsGuests).toBeDefined();
        expect(initialExercises).toBeDefined();
        expect(initialRecommendations).toBeDefined();
        expect(initialHardening).toBeDefined();
        expect(initialAbout).toBeDefined();
        expect(initialNewbies).toBeDefined();
        expect(initialHeartVoice).toBeDefined();
        expect(initialReflection).toBeDefined();
        expect(initialInterviews).toBeDefined();
        expect(initialParticipants).toBeDefined();
        expect(initialMotivation).toBeDefined();
        expect(initialReviews).toBeDefined();
        expect(initialMusic).toBeDefined();
        expect(initialMovies).toBeDefined();
        expect(initialLiterature).toBeDefined();
        expect(initialAboutTimoshenko).toBeDefined();
        expect(initialChallenge).toBeDefined();
    });

    it('each data array has exactly 5 items', () => {
        const arrays = [
            initialBroadcasts, initialBroadcastsGuests, initialExercises,
            initialRecommendations, initialHardening, initialAbout,
            initialNewbies, initialHeartVoice, initialReflection,
            initialInterviews, initialParticipants, initialMotivation,
            initialReviews, initialMusic, initialMovies,
            initialLiterature, initialAboutTimoshenko, initialChallenge,
        ];

        arrays.forEach((arr, index) => {
            expect(arr.length).toBe(5);
        });
    });

    it('each item has required fields', () => {
        const arrays = [
            initialBroadcasts, initialBroadcastsGuests, initialExercises,
            initialRecommendations, initialHardening, initialAbout,
            initialNewbies, initialHeartVoice, initialReflection,
            initialInterviews, initialParticipants, initialMotivation,
            initialReviews, initialMusic, initialMovies,
            initialLiterature, initialAboutTimoshenko, initialChallenge,
        ];

        arrays.forEach(arr => {
            arr.forEach(item => {
                expect(item).toHaveProperty('id');
                expect(item).toHaveProperty('title');
                expect(item).toHaveProperty('description');
                expect(item).toHaveProperty('mediaUrl');
                expect(item).toHaveProperty('mediaType');
                expect(item).toHaveProperty('createdAt');
                expect(typeof item.id).toBe('number');
                expect(typeof item.title).toBe('string');
                expect(typeof item.description).toBe('string');
                expect(typeof item.mediaUrl).toBe('string');
                expect(typeof item.mediaType).toBe('string');
            });
        });
    });

    it('has sequential ids in each array', () => {
        const arrays = [
            initialBroadcasts, initialBroadcastsGuests, initialExercises,
            initialRecommendations, initialHardening, initialAbout,
            initialNewbies, initialHeartVoice, initialReflection,
            initialInterviews, initialParticipants, initialMotivation,
            initialReviews, initialMusic, initialMovies,
            initialLiterature, initialAboutTimoshenko, initialChallenge,
        ];

        arrays.forEach(arr => {
            arr.forEach((item, index) => {
                expect(item.id).toBe(index + 1);
            });
        });
    });

    it('initialBroadcasts has correct first item', () => {
        expect(initialBroadcasts[0].title).toContain('Основы здорового питания');
        expect(initialBroadcasts[0].mediaType).toBe('audio');
    });

    it('initialMusic items have audio mediaType', () => {
        initialMusic.forEach(item => {
            expect(item.mediaType).toBe('audio');
        });
    });

    it('initialMovies items have video mediaType', () => {
        initialMovies.forEach(item => {
            expect(item.mediaType).toBe('video');
        });
    });
});
